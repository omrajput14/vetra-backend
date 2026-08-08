package app.vetra.ai.workflow.clinical.model.explainability;

import java.math.BigDecimal;
import java.util.List;

/**
 * Structured quantification of clinical uncertainty and missing evidence modalities.
 *
 * @param overallLevel calculated uncertainty tier
 * @param confidenceScore top diagnostic confidence score
 * @param uncertaintyFactors specific indicators causing uncertainty
 * @param missingModalities list of evidence modalities not provided in context
 */
public record ClinicalUncertainty(
    UncertaintyLevel overallLevel,
    BigDecimal confidenceScore,
    List<String> uncertaintyFactors,
    List<String> missingModalities) {

  /** Canonical constructor with non-null defaults. */
  public ClinicalUncertainty {
    overallLevel = overallLevel != null ? overallLevel : UncertaintyLevel.INSUFFICIENT_EVIDENCE;
    confidenceScore = confidenceScore != null ? confidenceScore : BigDecimal.ZERO;
    uncertaintyFactors = uncertaintyFactors != null ? List.copyOf(uncertaintyFactors) : List.of();
    missingModalities = missingModalities != null ? List.copyOf(missingModalities) : List.of();
  }
}
