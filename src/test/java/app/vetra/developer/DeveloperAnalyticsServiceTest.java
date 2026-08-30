package app.vetra.developer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.vetra.ai.entity.AIScanStatus;
import app.vetra.ai.repository.AIScanRepository;
import app.vetra.animal.repository.AnimalHealthRecordRepository;
import app.vetra.animal.repository.AnimalRepository;
import app.vetra.appointment.repository.AppointmentChatMessageRepository;
import app.vetra.appointment.repository.AppointmentRepository;
import app.vetra.auth.repository.FarmerProfileRepository;
import app.vetra.auth.repository.RefreshTokenRepository;
import app.vetra.auth.repository.UserRepository;
import app.vetra.auth.repository.VetProfileRepository;
import app.vetra.developer.dto.DeveloperAuthAnalyticsDto;
import app.vetra.developer.dto.DeveloperOverviewDto;
import app.vetra.developer.dto.DeveloperSettingsDto;
import app.vetra.developer.dto.DeveloperSystemHealthDto;
import app.vetra.developer.dto.TelemetryClassification;
import app.vetra.developer.service.ClinicalDataHolder;
import app.vetra.developer.service.DeveloperAnalyticsService;
import app.vetra.developer.service.DeveloperFarmerVetAnalyticsService;
import app.vetra.developer.service.DeveloperFeatureService;
import app.vetra.developer.service.DeveloperUserDirectoryService;
import app.vetra.developer.service.SurveillanceDataHolder;
import app.vetra.disease.entity.DiagnosisStatus;
import app.vetra.disease.repository.DiseaseReportRepository;
import app.vetra.disease.repository.OutbreakRepository;
import app.vetra.infrastructure.persistence.enums.AppointmentStatus;
import app.vetra.infrastructure.persistence.enums.UserRole;
import app.vetra.medicalrecord.repository.MedicalRecordRepository;
import app.vetra.notification.repository.NotificationRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeveloperAnalyticsServiceTest {

  private UserRepository userRepository;
  private FarmerProfileRepository farmerProfileRepository;
  private VetProfileRepository vetProfileRepository;
  private AnimalRepository animalRepository;
  private AnimalHealthRecordRepository healthRecordRepository;
  private AppointmentRepository appointmentRepository;
  private MedicalRecordRepository medicalRecordRepository;
  private DiseaseReportRepository diseaseReportRepository;
  private AIScanRepository aiScanRepository;
  private NotificationRepository notificationRepository;
  private RefreshTokenRepository refreshTokenRepository;
  private SimpleMeterRegistry meterRegistry;
  private DeveloperAnalyticsService service;

  @BeforeEach
  void setUp() {
    userRepository = mock(UserRepository.class);
    farmerProfileRepository = mock(FarmerProfileRepository.class);
    vetProfileRepository = mock(VetProfileRepository.class);
    animalRepository = mock(AnimalRepository.class);
    healthRecordRepository = mock(AnimalHealthRecordRepository.class);
    appointmentRepository = mock(AppointmentRepository.class);
    medicalRecordRepository = mock(MedicalRecordRepository.class);
    diseaseReportRepository = mock(DiseaseReportRepository.class);
    aiScanRepository = mock(AIScanRepository.class);
    notificationRepository = mock(NotificationRepository.class);
    refreshTokenRepository = mock(RefreshTokenRepository.class);
    meterRegistry = new SimpleMeterRegistry();

    ClinicalDataHolder clinicalData =
        new ClinicalDataHolder(
            animalRepository,
            healthRecordRepository,
            appointmentRepository,
            mock(AppointmentChatMessageRepository.class),
            medicalRecordRepository);

    SurveillanceDataHolder surveillanceData =
        new SurveillanceDataHolder(
            diseaseReportRepository,
            mock(OutbreakRepository.class),
            aiScanRepository,
            mock(app.vetra.ai.repository.AIAdvisorSessionRepository.class),
            notificationRepository);

    DeveloperUserDirectoryService userDirectoryService =
        new DeveloperUserDirectoryService(
            userRepository, farmerProfileRepository, vetProfileRepository, refreshTokenRepository);

    DeveloperFarmerVetAnalyticsService farmerVetAnalyticsService =
        new DeveloperFarmerVetAnalyticsService(
            userRepository,
            farmerProfileRepository,
            vetProfileRepository,
            clinicalData,
            diseaseReportRepository,
            aiScanRepository);

    DeveloperFeatureService featureService =
        new DeveloperFeatureService(
            userRepository, vetProfileRepository, clinicalData, surveillanceData);

    service =
        new DeveloperAnalyticsService(
            userRepository,
            clinicalData,
            surveillanceData,
            refreshTokenRepository,
            meterRegistry,
            userDirectoryService,
            farmerVetAnalyticsService,
            featureService);
  }

  @Test
  @DisplayName("Should aggregate overview KPIs from repositories correctly")
  void shouldAggregateOverviewKPIs() {
    when(userRepository.count()).thenReturn(10L);
    when(userRepository.countByRole(UserRole.FARMER)).thenReturn(6L);
    when(userRepository.countByRole(UserRole.VETERINARIAN)).thenReturn(3L);
    when(userRepository.countByRole(UserRole.ADMINISTRATOR)).thenReturn(1L);
    when(userRepository.countByIsActiveTrue()).thenReturn(9L);
    when(userRepository.countByIsActiveFalse()).thenReturn(1L);

    when(animalRepository.count()).thenReturn(24L);
    when(appointmentRepository.count()).thenReturn(8L);
    when(appointmentRepository.countByStatus(AppointmentStatus.PENDING)).thenReturn(2L);
    when(appointmentRepository.countByStatus(AppointmentStatus.COMPLETED)).thenReturn(5L);
    when(appointmentRepository.countByStatus(AppointmentStatus.CANCELLED)).thenReturn(1L);

    when(diseaseReportRepository.count()).thenReturn(4L);
    when(diseaseReportRepository.countByDiagnosisStatus(DiagnosisStatus.SUSPECTED)).thenReturn(2L);
    when(diseaseReportRepository.countByDiagnosisStatus(DiagnosisStatus.CONFIRMED)).thenReturn(2L);

    when(aiScanRepository.count()).thenReturn(12L);
    when(aiScanRepository.countByStatus(AIScanStatus.VERIFIED)).thenReturn(8L);

    when(medicalRecordRepository.count()).thenReturn(5L);
    when(notificationRepository.count()).thenReturn(30L);
    when(refreshTokenRepository.countByRevokedFalseAndExpiryDateAfter(any(Instant.class))).thenReturn(3L);

    DeveloperOverviewDto overview = service.getOverview("ALL");

    assertNotNull(overview);
    assertEquals(10L, overview.totalUsers());
    assertEquals(6L, overview.farmersCount());
    assertEquals(3L, overview.veterinariansCount());
    assertEquals(24L, overview.totalAnimals());
    assertEquals(8L, overview.totalAppointments());
    assertEquals(3L, overview.activeSessionsCount());
    assertEquals("UP", overview.systemStatus());
    assertEquals(TelemetryClassification.REAL_BACKEND_DATA, overview.telemetrySources().get("users"));
  }

  @Test
  @DisplayName("Should provide safe authentication telemetry without exposing token strings")
  void shouldProvideSafeAuthTelemetry() {
    when(refreshTokenRepository.countByRevokedFalseAndExpiryDateAfter(any(Instant.class))).thenReturn(4L);
    when(refreshTokenRepository.countByRevokedTrue()).thenReturn(1L);
    when(refreshTokenRepository.count()).thenReturn(10L);
    when(refreshTokenRepository.findTop50ByOrderByCreatedAtDesc()).thenReturn(Collections.emptyList());

    DeveloperAuthAnalyticsDto authDto = service.getAuthAnalytics();

    assertNotNull(authDto);
    assertEquals(4L, authDto.activeSessionsCount());
    assertEquals(1L, authDto.revokedSessionsCount());
    assertEquals(10L, authDto.totalTokensIssued());
    assertTrue(authDto.telemetryNotes().containsKey("historicalIpAudit"));
  }

  @Test
  @DisplayName("Should return valid system health and settings")
  void shouldReturnSystemHealthAndSettings() {
    DeveloperSystemHealthDto health = service.getSystemHealth();
    assertNotNull(health);
    assertEquals("UP", health.status());
    assertEquals("ALIVE", health.liveness());

    DeveloperSettingsDto settings = service.getSettings();
    assertNotNull(settings);
    assertEquals("PostgreSQL 16 + PostGIS 3.4 (Hibernate Spatial)", settings.databaseType());
    assertTrue(settings.corsAllowedOrigins().contains("http://localhost:3001"));
  }
}
