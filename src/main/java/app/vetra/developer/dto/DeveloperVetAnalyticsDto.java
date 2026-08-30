package app.vetra.developer.dto;

import java.util.List;
import java.util.Map;

/** Veterinarian analytics deep-dive payload. */
public record DeveloperVetAnalyticsDto(
    long totalVeterinarians,
    long activeVeterinarians,
    long verifiedVeterinarians,
    long pendingVeterinarians,
    long rejectedVeterinarians,
    long availableVeterinarians,
    long emergencyAvailableVeterinarians,
    long totalAppointmentsReceived,
    long appointmentsCompleted,
    long appointmentsCancelled,
    long appointmentsRejected,
    long totalMedicalRecordsWritten,
    double averageYearsExperience,
    Map<String, Long> specializationDistribution,
    Map<String, Long> topDistricts,
    List<TimeSeriesPointDto> registrationTrend,
    String timeRange) {}
