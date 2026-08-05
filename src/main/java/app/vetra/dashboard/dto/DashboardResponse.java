package app.vetra.dashboard.dto;

/** Unified response DTO for dashboard metrics in a single API request. */
public record DashboardResponse(
    long registeredAnimalCount,
    long pendingAppointmentsCount,
    long activeAlertsCount,
    long medicalRecordsCreatedCount,
    String userName,
    String facilityName,
    String role) {}
