package app.vetra.ai.event;

import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published upon completion of the clinical triage and urgency assessment step.
 *
 * @param scanId scan entity identifier
 * @param animalId target animal identifier
 * @param urgency determined triage urgency level
 * @param timestamp event timestamp
 * @param durationMs triage execution latency in milliseconds
 */
public record ClinicalTriageCompletedEvent(
    UUID scanId,
    UUID animalId,
    TriageUrgency urgency,
    Instant timestamp,
    long durationMs) {

  /** Canonical constructor with non-null defaults. */
  public ClinicalTriageCompletedEvent {
    urgency = urgency != null ? urgency : TriageUrgency.ROUTINE;
    timestamp = timestamp != null ? timestamp : Instant.now();
    if (durationMs < 0) {
      durationMs = 0;
    }
  }
}
