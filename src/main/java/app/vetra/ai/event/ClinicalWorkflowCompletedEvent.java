package app.vetra.ai.event;

import app.vetra.ai.workflow.clinical.model.ClinicalDiagnosisReport;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Domain event published when a multi-agent clinical diagnosis workflow completes successfully.
 *
 * <p>Enables asynchronous listeners to handle result persistence, notifications, and analytics
 * without coupling the workflow engine directly to data stores.
 *
 * @param scanId scan entity identifier
 * @param animalId target animal identifier
 * @param reportId unique report identifier
 * @param primaryDiagnosis name of the highest-ranked disease condition
 * @param confidenceScore normalized confidence score
 * @param durationMs total workflow latency in milliseconds
 * @param report complete clinical report
 */
public record ClinicalWorkflowCompletedEvent(
    UUID scanId,
    UUID animalId,
    UUID reportId,
    String primaryDiagnosis,
    BigDecimal confidenceScore,
    long durationMs,
    ClinicalDiagnosisReport report) {

  /** Canonical constructor with non-null defaults. */
  public ClinicalWorkflowCompletedEvent {
    primaryDiagnosis = primaryDiagnosis != null ? primaryDiagnosis.trim() : "Unspecified Observation";
    confidenceScore = confidenceScore != null ? confidenceScore : BigDecimal.valueOf(0.10);
    if (durationMs < 0) {
      durationMs = 0;
    }
  }
}
