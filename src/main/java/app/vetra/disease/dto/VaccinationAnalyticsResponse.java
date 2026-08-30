package app.vetra.disease.dto;

import java.util.List;
import java.util.UUID;

/** Response DTO representing aggregated regional and pathogen-level vaccination intelligence. */
public record VaccinationAnalyticsResponse(
    long totalRegisteredLivestock,
    long totalVaccinatedLivestock,
    long totalUnvaccinatedLivestock,
    double overallCoveragePercentage,
    double overallImmunityGapPercentage,
    List<PathogenCoverageDto> pathogenCoverage,
    List<ZoneVaccinationGapDto> zoneVaccinationGaps,
    List<PriorityImmunityDeficitZoneDto> priorityDeficitZones) {

  /** Pathogen-specific vaccination coverage metrics. */
  public record PathogenCoverageDto(
      String diseaseName,
      long vaccinatedCount,
      long eligibleCount,
      double coveragePercentage,
      double immunityGapPercentage,
      double targetCoveragePercentage,
      String status) {}

  /** Outbreak cluster geographic zone vaccination gap metrics. */
  public record ZoneVaccinationGapDto(
      UUID outbreakId,
      String zoneName,
      String diseaseName,
      double latitude,
      double longitude,
      double radiusKm,
      int totalAnimals,
      int vaccinatedAnimals,
      double coveragePercentage,
      double immunityGapPercentage,
      String riskLevel) {}

  /** Priority deficit zone requiring targeted containment and ring vaccination. */
  public record PriorityImmunityDeficitZoneDto(
      UUID outbreakId,
      String zoneName,
      String primaryDisease,
      double immunityGapPercentage,
      double outbreakRiskScore,
      String operationalPriority,
      String recommendedAction) {}
}
