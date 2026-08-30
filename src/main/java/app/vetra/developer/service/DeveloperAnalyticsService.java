package app.vetra.developer.service;

import app.vetra.ai.entity.AIScanStatus;
import app.vetra.auth.repository.RefreshTokenRepository;
import app.vetra.auth.repository.UserRepository;
import app.vetra.developer.dto.DeveloperActivityEventDto;
import app.vetra.developer.dto.DeveloperApiPerformanceDto;
import app.vetra.developer.dto.DeveloperAuthAnalyticsDto;
import app.vetra.developer.dto.DeveloperFarmerAnalyticsDto;
import app.vetra.developer.dto.DeveloperFeatureUsageDto;
import app.vetra.developer.dto.DeveloperOverviewDto;
import app.vetra.developer.dto.DeveloperSettingsDto;
import app.vetra.developer.dto.DeveloperSystemHealthDto;
import app.vetra.developer.dto.DeveloperUserPageResponse;
import app.vetra.developer.dto.DeveloperVetAnalyticsDto;
import app.vetra.developer.dto.TelemetryClassification;
import app.vetra.disease.entity.DiagnosisStatus;
import app.vetra.infrastructure.persistence.entity.RefreshToken;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.enums.AppointmentStatus;
import app.vetra.infrastructure.persistence.enums.UserRole;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Enterprise analytics facade for the VETRA Developer Console.
 * Coordinates telemetry extraction with zero credential exposure.
 */
@Service
public class DeveloperAnalyticsService {

  private final UserRepository userRepository;
  private final ClinicalDataHolder clinicalData;
  private final SurveillanceDataHolder surveillanceData;
  private final RefreshTokenRepository refreshTokenRepository;
  private final MeterRegistry meterRegistry;
  private final DeveloperUserDirectoryService userDirectoryService;
  private final DeveloperFarmerVetAnalyticsService farmerVetAnalyticsService;
  private final DeveloperFeatureService featureService;
  private final DeveloperActivityFeedService activityFeedService;
  private final DeveloperSystemTelemetryService systemTelemetryService;

  @Value("${info.app.version:0.12.5.0}")
  private String version = "0.12.5.0";

  @Value("${spring.profiles.active:dev}")
  private String environment = "dev";

  public DeveloperAnalyticsService(
      UserRepository userRepository,
      ClinicalDataHolder clinicalData,
      SurveillanceDataHolder surveillanceData,
      RefreshTokenRepository refreshTokenRepository,
      MeterRegistry meterRegistry,
      DeveloperUserDirectoryService userDirectoryService,
      DeveloperFarmerVetAnalyticsService farmerVetAnalyticsService,
      DeveloperFeatureService featureService) {
    this.userRepository = userRepository;
    this.clinicalData = clinicalData;
    this.surveillanceData = surveillanceData;
    this.refreshTokenRepository = refreshTokenRepository;
    this.meterRegistry = meterRegistry;
    this.userDirectoryService = userDirectoryService;
    this.farmerVetAnalyticsService = farmerVetAnalyticsService;
    this.featureService = featureService;
    this.activityFeedService =
        new DeveloperActivityFeedService(
            userRepository,
            surveillanceData.scanRepo(),
            surveillanceData.reportRepo(),
            clinicalData.appointmentRepo(),
            clinicalData.medicalRecordRepo());
    this.systemTelemetryService = new DeveloperSystemTelemetryService(meterRegistry);
  }

  /** Section 1: Overview KPIs. */
  @Transactional(readOnly = true)
  public DeveloperOverviewDto getOverview(String timeRange) {
    long totalUsers = userRepository.count();
    long farmersCount = userRepository.countByRole(UserRole.FARMER);
    long veterinariansCount = userRepository.countByRole(UserRole.VETERINARIAN);
    long governmentOfficersCount = userRepository.countByRole(UserRole.GOVERNMENT_OFFICER);
    long administratorsCount = userRepository.countByRole(UserRole.ADMINISTRATOR);
    long activeUsersCount = userRepository.countByIsActiveTrue();
    long inactiveUsersCount = userRepository.countByIsActiveFalse();

    long totalAnimals = clinicalData.animalRepo().count();
    long totalAppointments = clinicalData.appointmentRepo().count();
    long pendingAppointments = clinicalData.appointmentRepo().countByStatus(AppointmentStatus.PENDING);
    long completedAppointments = clinicalData.appointmentRepo().countByStatus(AppointmentStatus.COMPLETED);
    long cancelledAppointments = clinicalData.appointmentRepo().countByStatus(AppointmentStatus.CANCELLED);

    long totalDiseaseReports = surveillanceData.reportRepo().count();
    long suspectedReports = surveillanceData.reportRepo().countByDiagnosisStatus(DiagnosisStatus.SUSPECTED);
    long confirmedReports = surveillanceData.reportRepo().countByDiagnosisStatus(DiagnosisStatus.CONFIRMED);

    long totalAiScans = surveillanceData.scanRepo().count();
    long verifiedAiScans = surveillanceData.scanRepo().countByStatus(AIScanStatus.VERIFIED);
    long totalMedicalRecords = clinicalData.medicalRecordRepo().count();
    long totalNotifications = surveillanceData.notificationRepo().count();

    long activeSessionsCount =
        refreshTokenRepository.countByRevokedFalseAndExpiryDateAfter(Instant.now());

    long authSuccessCount =
        getCounterValue("vetra.auth.login", "role", "FARMER", "result", "success")
            + getCounterValue("vetra.auth.login", "role", "VETERINARIAN", "result", "success");
    long authFailureCount =
        getCounterValue("vetra.auth.login", "role", "FARMER", "result", "failure")
            + getCounterValue("vetra.auth.login", "role", "VETERINARIAN", "result", "failure");
    long totalAttempts = authSuccessCount + authFailureCount;
    double authSuccessRate = totalAttempts > 0 ? (double) authSuccessCount / totalAttempts * 100.0 : 100.0;

    Map<String, TelemetryClassification> telemetrySources = new LinkedHashMap<>();
    telemetrySources.put("users", TelemetryClassification.REAL_BACKEND_DATA);
    telemetrySources.put("animals", TelemetryClassification.REAL_BACKEND_DATA);
    telemetrySources.put("appointments", TelemetryClassification.REAL_BACKEND_DATA);
    telemetrySources.put("diseaseReports", TelemetryClassification.REAL_BACKEND_DATA);
    telemetrySources.put("aiScans", TelemetryClassification.REAL_BACKEND_DATA);
    telemetrySources.put("medicalRecords", TelemetryClassification.REAL_BACKEND_DATA);
    telemetrySources.put("authTelemetry", TelemetryClassification.REAL_BACKEND_DATA);
    telemetrySources.put("activeSessions", TelemetryClassification.DERIVED_FROM_EXISTING_DATA);

    long uptimeSeconds = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;

    return new DeveloperOverviewDto(
        totalUsers,
        farmersCount,
        veterinariansCount,
        governmentOfficersCount,
        administratorsCount,
        activeUsersCount,
        inactiveUsersCount,
        totalAnimals,
        totalAppointments,
        pendingAppointments,
        completedAppointments,
        cancelledAppointments,
        totalDiseaseReports,
        suspectedReports,
        confirmedReports,
        totalAiScans,
        verifiedAiScans,
        totalMedicalRecords,
        totalNotifications,
        activeSessionsCount,
        authSuccessCount,
        authFailureCount,
        authSuccessRate,
        "UP",
        uptimeSeconds,
        version,
        environment,
        timeRange != null ? timeRange : "ALL",
        telemetrySources);
  }

  /** Section 2: User directory delegation. */
  @Transactional(readOnly = true)
  public DeveloperUserPageResponse getUsers(
      UserRole role, Boolean isActive, String search, int page, int size) {
    return userDirectoryService.getUsers(role, isActive, search, page, size);
  }

  /** Section 3: Authentication telemetry. */
  @Transactional(readOnly = true)
  public DeveloperAuthAnalyticsDto getAuthAnalytics() {
    long farmerSuccess = getCounterValue("vetra.auth.login", "role", "FARMER", "result", "success");
    long farmerFailure = getCounterValue("vetra.auth.login", "role", "FARMER", "result", "failure");
    long vetSuccess = getCounterValue("vetra.auth.login", "role", "VETERINARIAN", "result", "success");
    long vetFailure = getCounterValue("vetra.auth.login", "role", "VETERINARIAN", "result", "failure");

    long successfulLogins = farmerSuccess + vetSuccess;
    long failedLogins = farmerFailure + vetFailure;
    long totalAttempts = successfulLogins + failedLogins;
    double successRate = totalAttempts > 0 ? (double) successfulLogins / totalAttempts * 100.0 : 100.0;

    Instant now = Instant.now();
    long activeSessions = refreshTokenRepository.countByRevokedFalseAndExpiryDateAfter(now);
    long revokedSessions = refreshTokenRepository.countByRevokedTrue();
    long totalTokensIssued = refreshTokenRepository.count();

    List<RefreshToken> rawTokens = refreshTokenRepository.findTop50ByOrderByCreatedAtDesc();
    List<DeveloperAuthAnalyticsDto.SafeSessionMetadataDto> sessionDtos = new ArrayList<>();

    for (RefreshToken t : rawTokens) {
      User u = t.getUser();
      String identifier = maskIdentifier(u.getEmail() != null ? u.getEmail() : u.getPhone());
      long sessionAgeSeconds = Duration.between(t.getCreatedAt(), now).getSeconds();
      boolean isExpired = t.getExpiryDate().isBefore(now);

      sessionDtos.add(
          new DeveloperAuthAnalyticsDto.SafeSessionMetadataDto(
              t.getId(),
              u.getId(),
              identifier,
              u.getRole(),
              t.getCreatedAt(),
              t.getExpiryDate(),
              t.isRevoked(),
              sessionAgeSeconds,
              isExpired));
    }

    Map<String, String> telemetryNotes = new LinkedHashMap<>();
    telemetryNotes.put("loginCounters", "Current authentication telemetry from in-memory Micrometer instruments.");
    telemetryNotes.put("activeSessions", "Derived from non-revoked and unexpired records in refresh_tokens table.");
    telemetryNotes.put("historicalIpAudit", "NOT CURRENTLY TRACKED — Individual client IP addresses and failed attempt headers are not persistently recorded in database.");

    return new DeveloperAuthAnalyticsDto(
        totalAttempts,
        successfulLogins,
        failedLogins,
        successRate,
        farmerSuccess,
        farmerFailure,
        vetSuccess,
        vetFailure,
        activeSessions,
        revokedSessions,
        totalTokensIssued,
        sessionDtos,
        telemetryNotes);
  }

  /** Section 4: Farmer analytics deep-dive. */
  @Transactional(readOnly = true)
  public DeveloperFarmerAnalyticsDto getFarmerAnalytics(String timeRange) {
    return farmerVetAnalyticsService.getFarmerAnalytics(timeRange);
  }

  /** Section 5: Veterinarian analytics deep-dive. */
  @Transactional(readOnly = true)
  public DeveloperVetAnalyticsDto getVetAnalytics(String timeRange) {
    return farmerVetAnalyticsService.getVetAnalytics(timeRange);
  }

  /** Section 6: Feature adoption matrix delegation. */
  @Transactional(readOnly = true)
  public DeveloperFeatureUsageDto getFeatureUsage() {
    return featureService.getFeatureUsage();
  }

  /** Section 7: Activity feed delegation. */
  @Transactional(readOnly = true)
  public List<DeveloperActivityEventDto> getActivityFeed(int limit) {
    return activityFeedService.getActivityFeed(limit);
  }

  /** Section 8: Live Spring Actuator system health and JVM telemetry. */
  public DeveloperSystemHealthDto getSystemHealth() {
    return systemTelemetryService.getSystemHealth();
  }

  /** Section 9: API performance telemetry from Micrometer. */
  public DeveloperApiPerformanceDto getApiPerformance() {
    return systemTelemetryService.getApiPerformance();
  }

  /** Section 10: Strict allowlist configuration for Developer Settings. */
  public DeveloperSettingsDto getSettings() {
    return systemTelemetryService.getSettings();
  }

  private long getCounterValue(String name, String... tags) {
    try {
      Counter counter = meterRegistry.find(name).tags(tags).counter();
      return counter != null ? (long) counter.count() : 0L;
    } catch (Exception e) {
      return 0L;
    }
  }

  private String maskIdentifier(String value) {
    if (value == null || value.isBlank()) {
      return "Anonymous";
    }
    if (value.contains("@")) {
      String[] parts = value.split("@");
      String name = parts[0];
      String masked = name.length() <= 2 ? name + "***" : name.substring(0, 2) + "***" + name.charAt(name.length() - 1);
      return masked + "@" + parts[1];
    }
    if (value.length() >= 7) {
      return value.substring(0, 3) + "****" + value.substring(value.length() - 2);
    }
    return value;
  }
}
