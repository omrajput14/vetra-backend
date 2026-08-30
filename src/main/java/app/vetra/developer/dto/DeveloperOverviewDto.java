package app.vetra.developer.dto;

import java.util.Map;

/** Consolidated platform overview KPI payload for Developer Console. */
public record DeveloperOverviewDto(
    long totalUsers,
    long farmersCount,
    long veterinariansCount,
    long governmentOfficersCount,
    long administratorsCount,
    long activeUsersCount,
    long inactiveUsersCount,
    long totalAnimals,
    long totalAppointments,
    long pendingAppointments,
    long completedAppointments,
    long cancelledAppointments,
    long totalDiseaseReports,
    long suspectedReports,
    long confirmedReports,
    long totalAiScans,
    long verifiedAiScans,
    long totalMedicalRecords,
    long totalNotifications,
    long activeSessionsCount,
    long authSuccessCount,
    long authFailureCount,
    double authSuccessRatePercent,
    String systemStatus,
    long uptimeSeconds,
    String version,
    String environment,
    String timeRange,
    Map<String, TelemetryClassification> telemetrySources) {}
