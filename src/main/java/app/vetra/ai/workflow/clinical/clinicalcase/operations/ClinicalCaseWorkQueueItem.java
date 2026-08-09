package app.vetra.ai.workflow.clinical.clinicalcase.operations;

import app.vetra.ai.workflow.clinical.clinicalcase.response.TreatmentResponseStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskPriority;
import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable queue entry projection for the case work queue.
 */
public record ClinicalCaseWorkQueueItem(
    UUID caseId,
    UUID animalId,
    CaseOperationalStatus operationalStatus,
    CareTaskPriority priority,
    TriageUrgency latestUrgency,
    boolean veterinarianReviewRequired,
    TreatmentResponseStatus treatmentResponseStatus,
    int openTaskCount,
    int overdueTaskCount,
    Instant nextDueAt,
    Instant lastEncounterAt,
    CaseWorkQueueReason queueReason,
    String nextOperationalAction) {

  /** Canonical constructor with non-null defaults. */
  public ClinicalCaseWorkQueueItem {
    caseId = caseId != null ? caseId : UUID.randomUUID();
    animalId = animalId != null ? animalId : UUID.randomUUID();
    operationalStatus = operationalStatus != null ? operationalStatus : CaseOperationalStatus.STABLE;
    priority = priority != null ? priority : CareTaskPriority.MEDIUM;
    latestUrgency = latestUrgency != null ? latestUrgency : TriageUrgency.ROUTINE;
    treatmentResponseStatus = treatmentResponseStatus != null ? treatmentResponseStatus : TreatmentResponseStatus.INSUFFICIENT_DATA;
    queueReason = queueReason != null ? queueReason : CaseWorkQueueReason.ROUTINE_CASE_REVIEW;
    nextOperationalAction = nextOperationalAction != null ? nextOperationalAction.trim() : "";
  }
}
