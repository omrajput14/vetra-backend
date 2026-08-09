package app.vetra.ai.event;

import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when clinical action plan synthesis completes.
 *
 * @param scanId linked scan identifier
 * @param animalId linked animal identifier
 * @param planId unique action plan identifier
 * @param urgency calculated triage urgency
 * @param veterinarianReviewRequired true if veterinarian review is required
 * @param actionCount total count of generated actions
 * @param timestamp event creation timestamp
 */
public record ClinicalActionPlanGeneratedEvent(
    UUID scanId,
    UUID animalId,
    UUID planId,
    TriageUrgency urgency,
    boolean veterinarianReviewRequired,
    int actionCount,
    Instant timestamp) {

  /** Canonical constructor with non-null defaults. */
  public ClinicalActionPlanGeneratedEvent {
    urgency = urgency != null ? urgency : TriageUrgency.ROUTINE;
    timestamp = timestamp != null ? timestamp : Instant.now();
  }
}
