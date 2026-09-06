package app.vetra.disease.dto;

/**
 * Summary DTO for epidemiological outbreak cluster statistics.
 *
 * @param totalOutbreaks total outbreak clusters
 * @param activeOutbreaks active outbreak clusters
 * @param criticalOutbreaks critical risk level clusters
 * @param highRiskOutbreaks high risk level clusters
 * @param totalAffectedReports total cumulative affected reports count
 * @param totalMortalities total cumulative livestock mortalities
 * @param vetConfirmedMortalities total veterinarian-confirmed mortalities
 */
public record OutbreakStatisticsResponse(
    long totalOutbreaks,
    long activeOutbreaks,
    long criticalOutbreaks,
    long highRiskOutbreaks,
    long totalAffectedReports,
    long totalMortalities,
    long vetConfirmedMortalities) {}

