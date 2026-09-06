package app.vetra.vaccination.dto;

/**
 * Aggregated statistics for vaccination campaigns across the state/region.
 */
public record VaccinationCampaignStatisticsResponse(
    long totalCampaigns,
    long plannedCampaigns,
    long activeCampaigns,
    long completedCampaigns,
    long cancelledCampaigns,
    long totalPlannedDoses,
    long totalAdministeredDoses,
    double overallProgressPercentage) {}
