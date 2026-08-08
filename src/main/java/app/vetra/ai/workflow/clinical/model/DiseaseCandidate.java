package app.vetra.ai.workflow.clinical.model;

import app.vetra.ai.rag.model.Citation;
import java.math.BigDecimal;
import java.util.List;

/**
 * Immutable candidate disease output produced by the DiseaseRanker.
 *
 * @param diseaseName clinical name of the identified disease
 * @param confidence calibrated confidence score normalized to [0.00, 1.00]
 * @param evidence synthesized clinical evidence justifying the ranking
 * @param citations structured veterinary literature citations
 * @param requiresUrgentReview whether critical veterinary intervention is required
 */
public record DiseaseCandidate(
    String diseaseName,
    BigDecimal confidence,
    String evidence,
    List<Citation> citations,
    boolean requiresUrgentReview) {

  /** Canonical constructor with non-null defaults. */
  public DiseaseCandidate {
    diseaseName = diseaseName != null ? diseaseName.trim() : "Unknown Condition";
    confidence = confidence != null ? confidence : BigDecimal.valueOf(0.10);
    evidence = evidence != null ? evidence.trim() : "";
    citations = citations != null ? List.copyOf(citations) : List.of();
  }
}
