package app.vetra.ai.workflow.clinical.model.explainability;

import app.vetra.ai.workflow.clinical.model.evidence.ClinicalEvidence;
import java.util.List;

/**
 * Summary of genuine evidence contradictions and literature discrepancies.
 *
 * @param conflictingMeasurements genuine measurement conflicts identified by aggregator
 * @param diagnosticContradictions list of evidence items contradicting the primary candidate
 * @param literatureDiscrepancies literature context discrepancies or low similarity warnings
 */
public record ContradictoryEvidenceSummary(
    List<String> conflictingMeasurements,
    List<ClinicalEvidence> diagnosticContradictions,
    List<String> literatureDiscrepancies) {

  /** Canonical constructor with non-null defaults. */
  public ContradictoryEvidenceSummary {
    conflictingMeasurements = conflictingMeasurements != null ? List.copyOf(conflictingMeasurements) : List.of();
    diagnosticContradictions = diagnosticContradictions != null ? List.copyOf(diagnosticContradictions) : List.of();
    literatureDiscrepancies = literatureDiscrepancies != null ? List.copyOf(literatureDiscrepancies) : List.of();
  }
}
