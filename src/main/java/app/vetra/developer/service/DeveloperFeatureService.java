package app.vetra.developer.service;

import app.vetra.auth.repository.UserRepository;
import app.vetra.auth.repository.VetProfileRepository;
import app.vetra.developer.dto.DeveloperFeatureUsageDto;
import app.vetra.developer.dto.DeveloperFeatureUsageDto.FeatureAdoptionItemDto;
import app.vetra.developer.dto.TelemetryClassification;
import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.infrastructure.persistence.enums.HealthRecordType;
import app.vetra.infrastructure.persistence.enums.UserRole;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service responsible for computing the feature adoption matrix. */
@Service
public class DeveloperFeatureService {

  private final UserRepository userRepository;
  private final VetProfileRepository vetProfileRepository;
  private final ClinicalDataHolder clinicalData;
  private final SurveillanceDataHolder surveillanceData;

  public DeveloperFeatureService(
      UserRepository userRepository,
      VetProfileRepository vetProfileRepository,
      ClinicalDataHolder clinicalData,
      SurveillanceDataHolder surveillanceData) {
    this.userRepository = userRepository;
    this.vetProfileRepository = vetProfileRepository;
    this.clinicalData = clinicalData;
    this.surveillanceData = surveillanceData;
  }

  @Transactional(readOnly = true)
  public DeveloperFeatureUsageDto getFeatureUsage() {
    List<FeatureAdoptionItemDto> items = new ArrayList<>();

    items.add(createDiseaseReportingItem());
    items.add(createAiScanItem());
    items.add(createAiAdvisorItem());
    items.add(createVetDiscoveryItem());
    items.add(createAppointmentBookingItem());
    items.add(createVaccinationTrackingItem());
    items.add(createAnimalPassportItem());
    items.add(createDiseaseMapItem());
    items.add(createNotificationsItem());
    items.add(createEmergencyResponseItem());
    items.add(createChatMessagingItem());
    items.add(createWeatherIntelligenceItem());

    long tracked = items.stream().filter(i -> i.classification() != TelemetryClassification.NOT_CURRENTLY_TRACKED).count();
    long untracked = items.size() - tracked;

    return new DeveloperFeatureUsageDto(items, tracked, untracked);
  }

  private FeatureAdoptionItemDto createDiseaseReportingItem() {
    long count = surveillanceData.reportRepo().count();
    long users = userRepository.countByRole(UserRole.FARMER) + userRepository.countByRole(UserRole.VETERINARIAN);
    return new FeatureAdoptionItemDto(
        "DISEASE_REPORTING",
        "Livestock Disease Reporting",
        "Surveillance",
        TelemetryClassification.REAL_BACKEND_DATA,
        count,
        users,
        "Farmer and veterinarian field disease incident submissions with GeoJSON spatial boundaries",
        "disease_reports table",
        surveillanceData.reportRepo().findTop30ByOrderByCreatedAtDesc().isEmpty() ? null : Instant.now().toString());
  }

  private FeatureAdoptionItemDto createAiScanItem() {
    return new FeatureAdoptionItemDto(
        "AI_DIAGNOSTIC_SCAN",
        "Computer Vision AI Diagnostic Scan",
        "Diagnostics",
        TelemetryClassification.REAL_BACKEND_DATA,
        surveillanceData.scanRepo().count(),
        null,
        "Multi-modal vision inference on livestock lesions and clinical presentations",
        "ai_scans table",
        surveillanceData.scanRepo().findTop30ByOrderByCreatedAtDesc().isEmpty() ? null : Instant.now().toString());
  }

  private FeatureAdoptionItemDto createAiAdvisorItem() {
    return new FeatureAdoptionItemDto(
        "AI_ADVISOR",
        "AI Veterinary Clinical Advisor",
        "Clinical",
        TelemetryClassification.REAL_BACKEND_DATA,
        surveillanceData.advisorRepo().count(),
        null,
        "Interactive veterinary advisory chat sessions with multi-turn clinical reasoning",
        "ai_advisor_sessions table",
        null);
  }

  private FeatureAdoptionItemDto createVetDiscoveryItem() {
    return new FeatureAdoptionItemDto(
        "VET_DISCOVERY",
        "Veterinarian Discovery & Proximity Matching",
        "Coordination",
        TelemetryClassification.DERIVED_FROM_EXISTING_DATA,
        clinicalData.appointmentRepo().count(),
        null,
        "Spatial GPS and administrative bounding-box search for nearby licensed veterinarians",
        "vet_profiles & appointments",
        null);
  }

  private FeatureAdoptionItemDto createAppointmentBookingItem() {
    return new FeatureAdoptionItemDto(
        "APPOINTMENT_BOOKING",
        "Clinical Appointment Scheduling",
        "Coordination",
        TelemetryClassification.REAL_BACKEND_DATA,
        clinicalData.appointmentRepo().count(),
        null,
        "Direct appointment booking, status lifecycle tracking, and home/clinic visit management",
        "appointments table",
        clinicalData.appointmentRepo().findTop30ByOrderByCreatedAtDesc().isEmpty() ? null : Instant.now().toString());
  }

  private FeatureAdoptionItemDto createVaccinationTrackingItem() {
    return new FeatureAdoptionItemDto(
        "VACCINATION_TRACKING",
        "Livestock Vaccination & Immunization Records",
        "Clinical",
        TelemetryClassification.REAL_BACKEND_DATA,
        clinicalData.healthRecordRepo().countByRecordType(HealthRecordType.VACCINATION),
        null,
        "Vaccination tracking with batch numbers, booster due dates, and certificate storage",
        "animal_health_records table",
        null);
  }

  private FeatureAdoptionItemDto createAnimalPassportItem() {
    return new FeatureAdoptionItemDto(
        "ANIMAL_PASSPORT",
        "Digital Animal Passport & Lifetime Timeline",
        "Clinical",
        TelemetryClassification.REAL_BACKEND_DATA,
        clinicalData.animalRepo().count() + clinicalData.healthRecordRepo().count(),
        null,
        "Lifetime health timeline aggregating vet visits, diagnoses, treatments, and vaccinations",
        "animals & animal_health_records",
        null);
  }

  private FeatureAdoptionItemDto createDiseaseMapItem() {
    return new FeatureAdoptionItemDto(
        "DISEASE_MAP",
        "Epidemiological Heatmap & Surveillance Map",
        "Surveillance",
        TelemetryClassification.REAL_BACKEND_DATA,
        surveillanceData.outbreakRepo().count(),
        null,
        "Spatial outbreak clustering, kernel density heatmaps, and GIS administrative layers",
        "outbreaks & disease_reports",
        null);
  }

  private FeatureAdoptionItemDto createNotificationsItem() {
    return new FeatureAdoptionItemDto(
        "NOTIFICATIONS",
        "FCM Push Notification Dispatcher",
        "Communications",
        TelemetryClassification.REAL_BACKEND_DATA,
        surveillanceData.notificationRepo().count(),
        null,
        "Automated alert delivery to Android/iOS mobile devices with delivery retry queue",
        "notifications & notification_delivery_logs",
        surveillanceData.notificationRepo().findTop30ByOrderByCreatedAtDesc().isEmpty() ? null : Instant.now().toString());
  }

  private FeatureAdoptionItemDto createEmergencyResponseItem() {
    long count = vetProfileRepository.findAll().stream().filter(VetProfile::isEmergencyAvailable).count();
    return new FeatureAdoptionItemDto(
        "EMERGENCY_RESPONSE",
        "Emergency Veterinary SOS Dispatch",
        "Coordination",
        TelemetryClassification.DERIVED_FROM_EXISTING_DATA,
        count,
        null,
        "On-demand emergency veterinary availability routing and priority matching",
        "vet_profiles.emergency_available",
        null);
  }

  private FeatureAdoptionItemDto createChatMessagingItem() {
    return new FeatureAdoptionItemDto(
        "APPOINTMENT_CHAT",
        "In-App Consultation Chat",
        "Communications",
        TelemetryClassification.REAL_BACKEND_DATA,
        clinicalData.chatRepo().count(),
        null,
        "Secure point-to-point chat messages between farmer and assigned veterinarian",
        "appointment_chat_messages table",
        null);
  }

  private FeatureAdoptionItemDto createWeatherIntelligenceItem() {
    return new FeatureAdoptionItemDto(
        "WEATHER_SIGNAL_INTELLIGENCE",
        "Weather Meteorological Risk Correlation",
        "Surveillance",
        TelemetryClassification.NOT_CURRENTLY_TRACKED,
        null,
        null,
        "External Open-Meteo meteorological query telemetry (cached in Redis, not persisted per-call)",
        "WeatherService cache",
        null);
  }
}
