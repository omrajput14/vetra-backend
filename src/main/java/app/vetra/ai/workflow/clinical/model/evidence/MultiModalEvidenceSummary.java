package app.vetra.ai.workflow.clinical.model.evidence;

import java.util.List;

/**
 * Structured summary of multi-modal evidence for inclusion in the ClinicalDiagnosisReport.
 *
 * @param totalEvidenceCount total count of evidence items aggregated
 * @param labCount laboratory result count
 * @param vitalCount vital sign measurement count
 * @param sensorCount sensor observation count
 * @param historyCount clinical history event count
 * @param visionCount visual image observation count
 * @param ragCount RAG literature reference count
 * @param criticalAbnormalities list of critical abnormality descriptions
 * @param conflicts list of detected measurement conflict descriptions
 * @param clinicalSummaryText controlled clinical summary string
 */
public record MultiModalEvidenceSummary(
    int totalEvidenceCount,
    int labCount,
    int vitalCount,
    int sensorCount,
    int historyCount,
    int visionCount,
    int ragCount,
    List<String> criticalAbnormalities,
    List<String> conflicts,
    String clinicalSummaryText) {

  /** Canonical constructor with non-null defaults. */
  public MultiModalEvidenceSummary {
    criticalAbnormalities = criticalAbnormalities != null ? List.copyOf(criticalAbnormalities) : List.of();
    conflicts = conflicts != null ? List.copyOf(conflicts) : List.of();
    clinicalSummaryText = clinicalSummaryText != null ? clinicalSummaryText.trim() : "";
  }
}
