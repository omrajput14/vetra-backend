package app.vetra.developer.service;

import app.vetra.ai.repository.AIScanRepository;
import app.vetra.animal.repository.AnimalRepository;
import app.vetra.appointment.repository.AppointmentRepository;
import app.vetra.auth.repository.FarmerProfileRepository;
import app.vetra.auth.repository.UserRepository;
import app.vetra.auth.repository.VetProfileRepository;
import app.vetra.developer.dto.DeveloperFarmerAnalyticsDto;
import app.vetra.developer.dto.DeveloperVetAnalyticsDto;
import app.vetra.developer.dto.TimeSeriesPointDto;
import app.vetra.disease.repository.DiseaseReportRepository;
import app.vetra.infrastructure.persistence.entity.FarmerProfile;
import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.infrastructure.persistence.enums.AppointmentStatus;
import app.vetra.infrastructure.persistence.enums.HealthRecordType;
import app.vetra.infrastructure.persistence.enums.Species;
import app.vetra.infrastructure.persistence.enums.UserRole;
import app.vetra.infrastructure.persistence.enums.VerificationStatus;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service responsible for Farmer and Veterinarian domain analytics. */
@Service
public class DeveloperFarmerVetAnalyticsService {

  private final UserRepository userRepository;
  private final FarmerProfileRepository farmerProfileRepository;
  private final VetProfileRepository vetProfileRepository;
  private final ClinicalDataHolder clinicalData;
  private final DiseaseReportRepository diseaseReportRepository;
  private final AIScanRepository aiScanRepository;

  public DeveloperFarmerVetAnalyticsService(
      UserRepository userRepository,
      FarmerProfileRepository farmerProfileRepository,
      VetProfileRepository vetProfileRepository,
      ClinicalDataHolder clinicalData,
      DiseaseReportRepository diseaseReportRepository,
      AIScanRepository aiScanRepository) {
    this.userRepository = userRepository;
    this.farmerProfileRepository = farmerProfileRepository;
    this.vetProfileRepository = vetProfileRepository;
    this.clinicalData = clinicalData;
    this.diseaseReportRepository = diseaseReportRepository;
    this.aiScanRepository = aiScanRepository;
  }

  @Transactional(readOnly = true)
  public DeveloperFarmerAnalyticsDto getFarmerAnalytics(String timeRange) {
    long totalFarmers = userRepository.countByRole(UserRole.FARMER);
    long activeFarmers =
        farmerProfileRepository.findAll().stream()
            .filter(f -> f.getUser() != null && f.getUser().isActive())
            .count();

    Instant windowStart = getWindowStart(timeRange);
    long newFarmersInPeriod =
        windowStart != null ? userRepository.countByCreatedAtAfter(windowStart) : totalFarmers;

    AnimalRepository animalRepo = clinicalData.animalRepo();
    long totalAnimals = animalRepo.count();
    Map<String, Long> speciesDistribution = new LinkedHashMap<>();
    for (Species s : Species.values()) {
      long count = animalRepo.countBySpecies(s);
      if (count > 0) {
        speciesDistribution.put(s.name(), count);
      }
    }

    long vaccinationRecords = clinicalData.healthRecordRepo().countByRecordType(HealthRecordType.VACCINATION);
    long vaccinated = Math.min(totalAnimals, vaccinationRecords);
    long unvaccinated = Math.max(0, totalAnimals - vaccinated);
    double vaccinationRate = totalAnimals > 0 ? (double) vaccinated / totalAnimals * 100.0 : 0.0;

    long diseaseReportsSubmitted = diseaseReportRepository.count();
    long aiScansSubmitted = aiScanRepository.count();
    long appointmentsBooked = clinicalData.appointmentRepo().count();

    Map<String, Long> topDistricts =
        farmerProfileRepository.findAll().stream()
            .filter(f -> f.getDistrict() != null && !f.getDistrict().isBlank())
            .collect(Collectors.groupingBy(FarmerProfile::getDistrict, Collectors.counting()))
            .entrySet()
            .stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

    List<TimeSeriesPointDto> trend = generateUserRegistrationTrend(UserRole.FARMER);

    return new DeveloperFarmerAnalyticsDto(
        totalFarmers,
        activeFarmers,
        newFarmersInPeriod,
        totalAnimals,
        speciesDistribution,
        vaccinated,
        unvaccinated,
        vaccinationRate,
        diseaseReportsSubmitted,
        aiScansSubmitted,
        appointmentsBooked,
        vaccinationRecords,
        topDistricts,
        trend,
        timeRange != null ? timeRange : "ALL");
  }

  @Transactional(readOnly = true)
  public DeveloperVetAnalyticsDto getVetAnalytics(String timeRange) {
    long totalVets = userRepository.countByRole(UserRole.VETERINARIAN);
    List<VetProfile> allVets = vetProfileRepository.findAll();

    long activeVets = allVets.stream().filter(v -> v.getUser() != null && v.getUser().isActive()).count();
    long verified = allVets.stream().filter(v -> v.getVerificationStatus() == VerificationStatus.VERIFIED).count();
    long pending = allVets.stream().filter(v -> v.getVerificationStatus() == VerificationStatus.PENDING).count();
    long rejected = allVets.stream().filter(v -> v.getVerificationStatus() == VerificationStatus.REJECTED).count();

    long available = allVets.stream().filter(VetProfile::isAvailable).count();
    long emergency = allVets.stream().filter(VetProfile::isEmergencyAvailable).count();

    AppointmentRepository appointmentRepo = clinicalData.appointmentRepo();
    long totalAppointments = appointmentRepo.count();
    long completed = appointmentRepo.countByStatus(AppointmentStatus.COMPLETED);
    long cancelled = appointmentRepo.countByStatus(AppointmentStatus.CANCELLED);
    long rejectedAppointments = appointmentRepo.countByStatus(AppointmentStatus.REJECTED);

    long totalMedicalRecords = clinicalData.medicalRecordRepo().count();

    double avgExperience =
        allVets.stream()
            .filter(v -> v.getYearsExperience() != null)
            .mapToInt(VetProfile::getYearsExperience)
            .average()
            .orElse(0.0);

    Map<String, Long> specializationDist =
        allVets.stream()
            .filter(v -> v.getSpecialization() != null && !v.getSpecialization().isBlank())
            .collect(Collectors.groupingBy(VetProfile::getSpecialization, Collectors.counting()));

    Map<String, Long> topDistricts =
        allVets.stream()
            .filter(v -> v.getDistrict() != null && !v.getDistrict().isBlank())
            .collect(Collectors.groupingBy(VetProfile::getDistrict, Collectors.counting()))
            .entrySet()
            .stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

    List<TimeSeriesPointDto> trend = generateUserRegistrationTrend(UserRole.VETERINARIAN);

    return new DeveloperVetAnalyticsDto(
        totalVets,
        activeVets,
        verified,
        pending,
        rejected,
        available,
        emergency,
        totalAppointments,
        completed,
        cancelled,
        rejectedAppointments,
        totalMedicalRecords,
        avgExperience,
        specializationDist,
        topDistricts,
        trend,
        timeRange != null ? timeRange : "ALL");
  }

  private Instant getWindowStart(String timeRange) {
    if ("TODAY".equalsIgnoreCase(timeRange) || "24H".equalsIgnoreCase(timeRange)) {
      return Instant.now().minus(Duration.ofDays(1));
    }
    if ("7D".equalsIgnoreCase(timeRange) || "7_DAYS".equalsIgnoreCase(timeRange)) {
      return Instant.now().minus(Duration.ofDays(7));
    }
    if ("30D".equalsIgnoreCase(timeRange) || "30_DAYS".equalsIgnoreCase(timeRange)) {
      return Instant.now().minus(Duration.ofDays(30));
    }
    return null;
  }

  private List<TimeSeriesPointDto> generateUserRegistrationTrend(UserRole role) {
    List<TimeSeriesPointDto> points = new ArrayList<>();
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd");

    for (int i = 6; i >= 0; i--) {
      LocalDate d = today.minusDays(i);
      points.add(new TimeSeriesPointDto(d.format(fmt), (long) (userRepository.countByRole(role) > 0 ? 1 : 0)));
    }
    return points;
  }
}
