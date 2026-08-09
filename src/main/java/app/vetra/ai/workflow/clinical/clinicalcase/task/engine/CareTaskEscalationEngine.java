package app.vetra.ai.workflow.clinical.clinicalcase.task.engine;

import app.vetra.ai.workflow.clinical.clinicalcase.coordination.FollowUpSchedule;
import app.vetra.ai.workflow.clinical.clinicalcase.coordination.FollowUpScheduleStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.encounter.ClinicalEncounter;
import app.vetra.ai.workflow.clinical.clinicalcase.response.TreatmentResponse;
import app.vetra.ai.workflow.clinical.clinicalcase.response.TreatmentResponseStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskActor;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskPriority;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskType;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.ClinicalCareTask;
import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import app.vetra.ai.workflow.clinical.model.explainability.ClinicalDecisionSupport;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Pure deterministic non-AI escalation engine evaluating active care tasks and follow-up schedules
 * against emergency conditions, critical evidence, worsening treatment response, and overdue tasks.
 */
@Component
public class CareTaskEscalationEngine {

  private static final Logger log = LoggerFactory.getLogger(CareTaskEscalationEngine.class);

  /**
   * Evaluates active tasks and follow-ups to determine which tasks require immediate escalation.
   *
   * @param activeTasks active tasks list
   * @param schedules active follow-up schedules list (optional)
   * @param latestEncounter latest encounter (optional)
   * @param decisionSupport decision support explainability output (optional)
   * @param treatmentResponse treatment response analysis (optional)
   * @param currentTime evaluation timestamp
   * @return list of escalated {@link ClinicalCareTask} objects
   */
  public List<ClinicalCareTask> evaluateEscalations(
      List<ClinicalCareTask> activeTasks,
      List<FollowUpSchedule> schedules,
      ClinicalEncounter latestEncounter,
      ClinicalDecisionSupport decisionSupport,
      TreatmentResponse treatmentResponse,
      Instant currentTime) {

    if (activeTasks == null || activeTasks.isEmpty()) {
      return List.of();
    }

    Instant now = currentTime != null ? currentTime : Instant.now();
    List<ClinicalCareTask> escalatedTasks = new ArrayList<>();

    for (ClinicalCareTask task : activeTasks) {
      if (task.status() == CareTaskStatus.COMPLETED || task.status() == CareTaskStatus.CANCELLED || task.status() == CareTaskStatus.ESCALATED) {
        continue;
      }

      boolean shouldEscalate = checkOverdueMandatoryVetTask(task, now)
          || checkWorseningResponse(task, treatmentResponse)
          || checkEmergencyEncounter(task, latestEncounter)
          || checkReviewRequired(task, decisionSupport)
          || checkMissedFollowUp(task, schedules);

      if (shouldEscalate) {
        ClinicalCareTask escalated = new ClinicalCareTask(
            task.taskId(),
            task.caseId(),
            task.sourceEncounterId(),
            task.type(),
            CareTaskPriority.EMERGENCY,
            CareTaskActor.VETERINARIAN,
            CareTaskStatus.ESCALATED,
            "ESCALATED: " + task.title(),
            task.description() + " [Escalation triggered at " + now + "]",
            task.createdAt(),
            task.dueAt(),
            task.completedAt(),
            true,
            true,
            true,
            task.supportingEvidence(),
            task.supportingCitations(),
            task.sourceActionId(),
            task.sourceFollowUpId(),
            task.provenance() + "_ESCALATED");
        escalatedTasks.add(escalated);
      }
    }

    log.info("CareTaskEscalationEngine evaluated {} active tasks; {} escalated.", activeTasks.size(), escalatedTasks.size());
    return escalatedTasks;
  }

  private boolean checkOverdueMandatoryVetTask(ClinicalCareTask task, Instant now) {
    return task.mandatory()
        && task.dueAt() != null
        && task.dueAt().isBefore(now)
        && (task.veterinarianRequired() || task.priority() == CareTaskPriority.HIGH || task.priority() == CareTaskPriority.EMERGENCY);
  }

  private boolean checkWorseningResponse(ClinicalCareTask task, TreatmentResponse response) {
    return response != null && response.status() == TreatmentResponseStatus.WORSENING && task.priority() != CareTaskPriority.EMERGENCY;
  }

  private boolean checkEmergencyEncounter(ClinicalCareTask task, ClinicalEncounter enc) {
    return enc != null && enc.urgency() == TriageUrgency.EMERGENCY && task.type() != CareTaskType.EMERGENCY_ESCALATION;
  }

  private boolean checkReviewRequired(ClinicalCareTask task, ClinicalDecisionSupport cds) {
    return cds != null && cds.veterinarianReviewFlag() != null && cds.veterinarianReviewFlag().requiresReview() && task.actor() != CareTaskActor.VETERINARIAN;
  }

  private boolean checkMissedFollowUp(ClinicalCareTask task, List<FollowUpSchedule> schedules) {
    if (schedules == null || task.sourceFollowUpId() == null) {
      return false;
    }
    return schedules.stream()
        .anyMatch(s -> s.followUpId().toString().equalsIgnoreCase(task.sourceFollowUpId())
            && (s.status() == FollowUpScheduleStatus.MISSED || s.status() == FollowUpScheduleStatus.OVERDUE));
  }
}
