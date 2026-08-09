package app.vetra.ai.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when clinical decision support & explainability evaluation completes.
 *
 * @param scanId scan identifier
 * @param animalId animal identifier
 * @param requiresReview true if veterinarian review is required
 * @param uncertaintyLevel calculated uncertainty level string
 * @param timestamp event creation timestamp
 */
public record ClinicalDecisionSupportGeneratedEvent(
    UUID scanId,
    UUID animalId,
    boolean requiresReview,
    String uncertaintyLevel,
    Instant timestamp) {

  /** Canonical constructor with non-null defaults. */
  public ClinicalDecisionSupportGeneratedEvent {
    uncertaintyLevel = uncertaintyLevel != null ? uncertaintyLevel : "INSUFFICIENT_EVIDENCE";
    timestamp = timestamp != null ? timestamp : Instant.now();
  }
}
