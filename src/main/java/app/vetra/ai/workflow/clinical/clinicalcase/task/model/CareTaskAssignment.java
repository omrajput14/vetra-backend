package app.vetra.ai.workflow.clinical.clinicalcase.task.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable audit record tracking assignment of a clinical care task to an actor.
 *
 * @param assignmentId unique assignment identifier
 * @param taskId linked task identifier
 * @param actor assigned responsible actor
 * @param assignedAt assignment timestamp
 * @param assignedBy assigner entity string
 * @param acceptedAt acceptance timestamp (optional)
 * @param completedAt completion timestamp (optional)
 */
public record CareTaskAssignment(
    UUID assignmentId,
    UUID taskId,
    CareTaskActor actor,
    Instant assignedAt,
    String assignedBy,
    Instant acceptedAt,
    Instant completedAt) {

  /** Canonical constructor with non-null defaults. */
  public CareTaskAssignment {
    assignmentId = assignmentId != null ? assignmentId : UUID.randomUUID();
    taskId = taskId != null ? taskId : UUID.randomUUID();
    actor = actor != null ? actor : CareTaskActor.CAREGIVER;
    assignedAt = assignedAt != null ? assignedAt : Instant.now();
    assignedBy = assignedBy != null ? assignedBy.trim() : "SYSTEM";
  }
}
