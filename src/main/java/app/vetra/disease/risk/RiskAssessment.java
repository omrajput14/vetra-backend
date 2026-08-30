package app.vetra.disease.risk;

import app.vetra.disease.entity.OutbreakRiskScore;
import java.time.Instant;

/**
 * Encapsulates the composite risk intelligence result for an outbreak cluster.
 */
public record RiskAssessment(
    int compositeScore,
    OutbreakRiskScore riskLevel,
    String diseaseName,
    SignalBreakdown breakdown,
    Instant evaluatedAt) {

  /** Factory method creating an evaluated RiskAssessment. */
  public static RiskAssessment of(
      int compositeScore,
      OutbreakRiskScore riskLevel,
      String diseaseName,
      SignalBreakdown breakdown) {
    return new RiskAssessment(compositeScore, riskLevel, diseaseName, breakdown, Instant.now());
  }
}
