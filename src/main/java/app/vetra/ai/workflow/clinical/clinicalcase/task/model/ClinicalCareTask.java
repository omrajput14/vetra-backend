package app.vetra.ai.workflow.clinical.clinicalcase.task.model;

import app.vetra.ai.rag.model.Citation;
import app.vetra.ai.workflow.clinical.model.evidence.ClinicalEvidence;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Immutable record representing an operational clinical care task.
 *
 * @param taskId unique task identifier
 * @param caseId linked case identifier
 * @param sourceEncounterId linked encounter identifier (optional)
 * @param type task type classification
 * @param priority precedence priority
 * @param actor responsible actor
 * @param status current lifecycle status
 * @param title concise task title
 * @param description detailed task description
 * @param createdAt creation timestamp
 * @param dueAt scheduled due timestamp (optional)
 * @param completedAt completion timestamp (optional)
 * @param mandatory true if task is mandatory
 * @param veterinarianRequired true if veterinarian attendance is required
 * @param escalationRequired true if escalation flag is active
 * @param supportingEvidence supporting evidence items
 * @param supportingCitations supporting literature citations
 * @param sourceActionId linked clinical action ID (optional)
 * @param sourceFollowUpId linked follow-up ID (optional)
 * @param provenance audit provenance metadata string
 */
public record ClinicalCareTask(
    UUID taskId,
    UUID caseId,
    UUID sourceEncounterId,
    CareTaskType type,
    CareTaskPriority priority,
    CareTaskActor actor,
    CareTaskStatus status,
    String title,
    String description,
    Instant createdAt,
    Instant dueAt,
    Instant completedAt,
    boolean mandatory,
    boolean veterinarianRequired,
    boolean escalationRequired,
    List<ClinicalEvidence> supportingEvidence,
    List<Citation> supportingCitations,
    String sourceActionId,
    String sourceFollowUpId,
    String provenance) {

  /** Canonical constructor with non-null defaults. */
  public ClinicalCareTask {
    taskId = taskId != null ? taskId : UUID.randomUUID();
    caseId = caseId != null ? caseId : UUID.randomUUID();
    type = type != null ? type : CareTaskType.MONITORING;
    priority = priority != null ? priority : CareTaskPriority.MEDIUM;
    actor = actor != null ? actor : CareTaskActor.CAREGIVER;
    status = status != null ? status : CareTaskStatus.PENDING;
    title = title != null ? title.trim() : "Clinical Care Task";
    description = description != null ? description.trim() : "";
    createdAt = createdAt != null ? createdAt : Instant.now();
    supportingEvidence = supportingEvidence != null ? List.copyOf(supportingEvidence) : List.of();
    supportingCitations = supportingCitations != null ? List.copyOf(supportingCitations) : List.of();
    provenance = provenance != null ? provenance.trim() : "CLINICAL_CARE_TASK_ENGINE";
  }
}
