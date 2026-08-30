package app.vetra.disease.dto;

import app.vetra.disease.entity.Outbreak;

/** DTO encapsulating the multi-signal risk breakdown for an outbreak cluster. */
public record RiskBreakdownResponse(
    Double clusterScore,
    Double weatherScore,
    Double historyScore,
    Double vaccinationGapScore,
    Double weatherTemperature,
    Double weatherHumidity,
    Double weatherPrecipitation,
    Double vaccinationCoveragePct,
    String riskExplanation,
    String recommendedAction) {

  /** Factory method mapping an Outbreak entity to RiskBreakdownResponse DTO. */
  public static RiskBreakdownResponse fromEntity(Outbreak outbreak) {
    return new RiskBreakdownResponse(
        outbreak.getClusterScore(),
        outbreak.getWeatherScore(),
        outbreak.getHistoryScore(),
        outbreak.getVaccinationGapScore(),
        outbreak.getWeatherTemperature(),
        outbreak.getWeatherHumidity(),
        outbreak.getWeatherPrecipitation(),
        outbreak.getVaccinationCoveragePct(),
        outbreak.getRiskExplanation(),
        outbreak.getRecommendedAction());
  }
}
