package app.vetra.ai.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a multi-agent clinical diagnosis workflow commences execution.
 *
 * @param scanId scan entity identifier
 * @param animalId target animal identifier
 * @param timestamp start timestamp
 */
public record ClinicalWorkflowStartedEvent(UUID scanId, UUID animalId, Instant timestamp) {

  /** Canonical constructor with non-null timestamp default. */
  public ClinicalWorkflowStartedEvent {
    timestamp = timestamp != null ? timestamp : Instant.now();
  }
}
