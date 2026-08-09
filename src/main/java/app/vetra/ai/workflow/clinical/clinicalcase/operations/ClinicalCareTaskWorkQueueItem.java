package app.vetra.ai.workflow.clinical.clinicalcase.operations;

import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskActor;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskPriority;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskType;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable task queue projection record for task-focused work queues.
 */
public record ClinicalCareTaskWorkQueueItem(
    UUID taskId,
    UUID caseId,
    UUID sourceEncounterId,
    CareTaskType taskType,
    CareTaskPriority priority,
    CareTaskActor actor,
    CareTaskStatus status,
    String title,
    Instant dueAt,
    boolean overdue,
    boolean mandatory,
    boolean veterinarianRequired,
    boolean escalationRequired,
    Instant createdAt) {

  /** Canonical constructor with non-null defaults. */
  public ClinicalCareTaskWorkQueueItem {
    taskId = taskId != null ? taskId : UUID.randomUUID();
    caseId = caseId != null ? caseId : UUID.randomUUID();
    taskType = taskType != null ? taskType : CareTaskType.MONITORING;
    priority = priority != null ? priority : CareTaskPriority.MEDIUM;
    actor = actor != null ? actor : CareTaskActor.CAREGIVER;
    status = status != null ? status : CareTaskStatus.PENDING;
    title = title != null ? title.trim() : "Clinical Care Task";
    createdAt = createdAt != null ? createdAt : Instant.now();
  }
}
