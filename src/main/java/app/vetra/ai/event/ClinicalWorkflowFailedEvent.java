package app.vetra.ai.event;

import java.util.UUID;

/**
 * Domain event published when a multi-agent clinical diagnosis workflow fails unrecoverably.
 *
 * @param scanId scan entity identifier
 * @param animalId target animal identifier
 * @param errorMessage detailed failure reason
 * @param failedStep name of the workflow step that failed
 * @param durationMs duration until failure in milliseconds
 */
public record ClinicalWorkflowFailedEvent(
    UUID scanId,
    UUID animalId,
    String errorMessage,
    String failedStep,
    long durationMs) {

  /** Canonical constructor with non-null defaults. */
  public ClinicalWorkflowFailedEvent {
    errorMessage = errorMessage != null ? errorMessage.trim() : "Unknown workflow failure";
    failedStep = failedStep != null ? failedStep.trim() : "UNKNOWN";
    if (durationMs < 0) {
      durationMs = 0;
    }
  }
}
