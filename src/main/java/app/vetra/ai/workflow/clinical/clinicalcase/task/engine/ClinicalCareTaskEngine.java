package app.vetra.ai.workflow.clinical.clinicalcase.task.engine;

import app.vetra.ai.rag.model.Citation;
import app.vetra.ai.workflow.clinical.clinicalcase.encounter.ClinicalEncounter;
import app.vetra.ai.workflow.clinical.clinicalcase.followup.ClinicalFollowUp;
import app.vetra.ai.workflow.clinical.clinicalcase.followup.FollowUpStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.model.ClinicalCase;
import app.vetra.ai.workflow.clinical.clinicalcase.response.TreatmentResponse;
import app.vetra.ai.workflow.clinical.clinicalcase.response.TreatmentResponseStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskActor;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskPriority;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskType;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.ClinicalCareTask;
import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import app.vetra.ai.workflow.clinical.model.action.ActionActor;
import app.vetra.ai.workflow.clinical.model.action.ActionPriority;
import app.vetra.ai.workflow.clinical.model.action.ActionType;
import app.vetra.ai.workflow.clinical.model.action.ClinicalAction;
import app.vetra.ai.workflow.clinical.model.action.ClinicalActionPlan;
import app.vetra.ai.workflow.clinical.model.evidence.ClinicalEvidence;
import app.vetra.ai.workflow.clinical.model.explainability.ClinicalDecisionSupport;
import app.vetra.ai.workflow.clinical.model.explainability.DiagnosticExplanation;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Pure deterministic non-AI engine synthesizing operational clinical care tasks
 * from existing structured clinical and longitudinal case state.
 */
@Component
public class ClinicalCareTaskEngine {

  private static final Logger log = LoggerFactory.getLogger(ClinicalCareTaskEngine.class);

  /**
   * Synthesizes prioritized, deduplicated care tasks from case state and encounter outputs.
   *
   * @param clinicalCase longitudinal clinical case
   * @param latestEncounter latest clinical encounter
   * @param actionPlan existing action plan (optional)
   * @param decisionSupport decision support explainability output (optional)
   * @param followUps active follow-ups list (optional)
   * @param treatmentResponse treatment response analysis (optional)
   * @return list of deterministic {@link ClinicalCareTask} objects sorted by priority
   */
  public List<ClinicalCareTask> generateCareTasks(
      ClinicalCase clinicalCase,
      ClinicalEncounter latestEncounter,
      ClinicalActionPlan actionPlan,
      ClinicalDecisionSupport decisionSupport,
      List<ClinicalFollowUp> followUps,
      TreatmentResponse treatmentResponse) {

    if (clinicalCase == null || latestEncounter == null) {
      log.debug("Missing clinicalCase or latestEncounter. Returning empty care tasks list.");
      return List.of();
    }

    Map<String, ClinicalCareTask> taskMap = new HashMap<>();

    generateEmergencyTaskIfRequired(clinicalCase, latestEncounter, decisionSupport, treatmentResponse, taskMap);
    generateVetReviewTaskIfRequired(clinicalCase, latestEncounter, decisionSupport, taskMap);
    generateWorseningResponseTaskIfRequired(clinicalCase, latestEncounter, treatmentResponse, taskMap);
    generateFollowUpTasks(clinicalCase, latestEncounter, followUps, taskMap);
    generateActionPlanTasks(clinicalCase, latestEncounter, actionPlan, decisionSupport, taskMap);

    List<ClinicalCareTask> sortedTasks = taskMap.values().stream()
        .sorted(getPriorityComparator())
        .toList();

    log.info("ClinicalCareTaskEngine generated {} care tasks for caseId={}", sortedTasks.size(), clinicalCase.caseId());
    return sortedTasks;
  }

  private void generateEmergencyTaskIfRequired(
      ClinicalCase cCase,
      ClinicalEncounter enc,
      ClinicalDecisionSupport cds,
      TreatmentResponse response,
      Map<String, ClinicalCareTask> taskMap) {

    boolean isEmergency = enc.urgency() == TriageUrgency.EMERGENCY
        || (response != null && response.status() == TreatmentResponseStatus.WORSENING && enc.urgency() == TriageUrgency.URGENT);

    if (isEmergency) {
      ClinicalCareTask task = new ClinicalCareTask(
          UUID.randomUUID(),
          cCase.caseId(),
          enc.encounterId(),
          CareTaskType.EMERGENCY_ESCALATION,
          CareTaskPriority.EMERGENCY,
          CareTaskActor.VETERINARIAN,
          CareTaskStatus.PENDING,
          "Emergency Veterinary Escalation",
          "Immediate emergency veterinary intervention required for " + enc.primaryDiagnosis(),
          Instant.now(),
          Instant.now(),
          null,
          true,
          true,
          true,
          getSupportingEvidence(cds),
          getSupportingCitations(cds),
          null,
          null,
          "CLINICAL_CARE_TASK_ENGINE_EMERGENCY");
      addTaskIfAbsent(taskMap, task);
    }
  }

  private void generateVetReviewTaskIfRequired(
      ClinicalCase cCase,
      ClinicalEncounter enc,
      ClinicalDecisionSupport cds,
      Map<String, ClinicalCareTask> taskMap) {

    boolean vetRequired = cds != null && cds.veterinarianReviewFlag() != null && cds.veterinarianReviewFlag().requiresReview();

    if (vetRequired) {
      String reason = (cds.veterinarianReviewFlag().reasons() != null && !cds.veterinarianReviewFlag().reasons().isEmpty())
          ? cds.veterinarianReviewFlag().reasons().get(0)
          : "Veterinarian review flagged by CDS";

      ClinicalCareTask task = new ClinicalCareTask(
          UUID.randomUUID(),
          cCase.caseId(),
          enc.encounterId(),
          CareTaskType.VETERINARIAN_REVIEW,
          CareTaskPriority.HIGH,
          CareTaskActor.VETERINARIAN,
          CareTaskStatus.PENDING,
          "Veterinarian Clinical Review Required",
          reason,
          Instant.now(),
          Instant.now().plusSeconds(7200),
          null,
          true,
          true,
          false,
          getSupportingEvidence(cds),
          getSupportingCitations(cds),
          null,
          null,
          "CLINICAL_CARE_TASK_ENGINE_VET_REVIEW");
      addTaskIfAbsent(taskMap, task);
    }
  }

  private void generateWorseningResponseTaskIfRequired(
      ClinicalCase cCase,
      ClinicalEncounter enc,
      TreatmentResponse response,
      Map<String, ClinicalCareTask> taskMap) {

    if (response != null && response.status() == TreatmentResponseStatus.WORSENING) {
      ClinicalCareTask task = new ClinicalCareTask(
          UUID.randomUUID(),
          cCase.caseId(),
          enc.encounterId(),
          CareTaskType.TREATMENT_REVIEW,
          CareTaskPriority.HIGH,
          CareTaskActor.VETERINARIAN,
          CareTaskStatus.PENDING,
          "Treatment Review - Condition Worsening",
          "Re-assess treatment protocol due to worsening condition indicators",
          Instant.now(),
          Instant.now().plusSeconds(14400),
          null,
          true,
          true,
          true,
          List.of(),
          List.of(),
          null,
          null,
          "CLINICAL_CARE_TASK_ENGINE_TREATMENT_REVIEW");
      addTaskIfAbsent(taskMap, task);
    }
  }

  private void generateFollowUpTasks(
      ClinicalCase cCase,
      ClinicalEncounter enc,
      List<ClinicalFollowUp> followUps,
      Map<String, ClinicalCareTask> taskMap) {

    if (followUps == null || followUps.isEmpty()) {
      return;
    }

    for (ClinicalFollowUp f : followUps) {
      if (f.status() == FollowUpStatus.SCHEDULED || f.status() == FollowUpStatus.DUE) {
        CareTaskPriority priority = f.status() == FollowUpStatus.DUE ? CareTaskPriority.HIGH : CareTaskPriority.MEDIUM;
        ClinicalCareTask task = new ClinicalCareTask(
            UUID.randomUUID(),
            cCase.caseId(),
            enc.encounterId(),
            CareTaskType.FOLLOW_UP,
            priority,
            CareTaskActor.CAREGIVER,
            f.status() == FollowUpStatus.DUE ? CareTaskStatus.DUE : CareTaskStatus.PENDING,
            "Clinical Follow-Up Inspection",
            f.reason(),
            Instant.now(),
            f.scheduledAt(),
            null,
            true,
            false,
            false,
            List.of(),
            List.of(),
            null,
            f.followUpId().toString(),
            "CLINICAL_CARE_TASK_ENGINE_FOLLOWUP");
        addTaskIfAbsent(taskMap, task);
      }
    }
  }

  private void generateActionPlanTasks(
      ClinicalCase cCase,
      ClinicalEncounter enc,
      ClinicalActionPlan actionPlan,
      ClinicalDecisionSupport cds,
      Map<String, ClinicalCareTask> taskMap) {

    List<ClinicalAction> allActions = getAllActions(actionPlan);
    if (allActions.isEmpty()) {
      return;
    }

    for (ClinicalAction action : allActions) {
      CareTaskType type = mapActionTypeToTaskType(action.type());
      CareTaskActor actor = mapActionActorToTaskActor(action.actor());
      CareTaskPriority priority = mapActionPriorityToTaskPriority(action.priority());

      ClinicalCareTask task = new ClinicalCareTask(
          UUID.randomUUID(),
          cCase.caseId(),
          enc.encounterId(),
          type,
          priority,
          actor,
          CareTaskStatus.PENDING,
          action.title(),
          action.description(),
          Instant.now(),
          action.dueAt() != null ? action.dueAt() : Instant.now().plusSeconds(86400),
          null,
          action.mandatory(),
          actor == CareTaskActor.VETERINARIAN,
          priority == CareTaskPriority.EMERGENCY,
          getSupportingEvidence(cds),
          getSupportingCitations(cds),
          action.actionId(),
          null,
          "CLINICAL_CARE_TASK_ENGINE_ACTION_PLAN");
      addTaskIfAbsent(taskMap, task);
    }
  }

  private List<ClinicalAction> getAllActions(ClinicalActionPlan plan) {
    if (plan == null) {
      return List.of();
    }
    List<ClinicalAction> list = new ArrayList<>();
    if (plan.immediateActions() != null) {
      list.addAll(plan.immediateActions());
    }
    if (plan.prioritizedActions() != null) {
      list.addAll(plan.prioritizedActions());
    }
    if (plan.monitoringActions() != null) {
      list.addAll(plan.monitoringActions());
    }
    if (plan.followUpActions() != null) {
      list.addAll(plan.followUpActions());
    }
    return list;
  }

  private void addTaskIfAbsent(Map<String, ClinicalCareTask> taskMap, ClinicalCareTask task) {
    String key = buildSemanticKey(task);
    if (!taskMap.containsKey(key)) {
      taskMap.put(key, task);
    }
  }

  private String buildSemanticKey(ClinicalCareTask task) {
    String actionId = task.sourceActionId() != null ? task.sourceActionId() : "NONE";
    String followUpId = task.sourceFollowUpId() != null ? task.sourceFollowUpId() : "NONE";
    return task.caseId() + ":" + task.type() + ":" + task.actor() + ":" + actionId + ":" + followUpId;
  }

  private CareTaskType mapActionTypeToTaskType(ActionType type) {
    if (type == null) {
      return CareTaskType.MONITORING;
    }
    return switch (type) {
      case VETERINARY_REFERRAL -> CareTaskType.REFERRAL;
      case IMMEDIATE_CARE -> CareTaskType.EMERGENCY_ESCALATION;
      case MEDICATION -> CareTaskType.TREATMENT_REVIEW;
      case DIAGNOSTIC_TEST -> CareTaskType.DIAGNOSTIC_TEST;
      case FOLLOW_UP -> CareTaskType.FOLLOW_UP;
      case MONITORING -> CareTaskType.MONITORING;
      case OWNER_NOTIFICATION -> CareTaskType.OWNER_CONTACT;
      case ISOLATION, PREVENTIVE_CARE -> CareTaskType.CASE_REVIEW;
    };
  }

  private CareTaskActor mapActionActorToTaskActor(ActionActor actor) {
    if (actor == null) {
      return CareTaskActor.CAREGIVER;
    }
    return switch (actor) {
      case VETERINARIAN -> CareTaskActor.VETERINARIAN;
      case FARMER, CAREGIVER -> CareTaskActor.CAREGIVER;
      case LABORATORY -> CareTaskActor.LABORATORY;
      case SYSTEM -> CareTaskActor.SYSTEM;
    };
  }

  private CareTaskPriority mapActionPriorityToTaskPriority(ActionPriority priority) {
    if (priority == null) {
      return CareTaskPriority.MEDIUM;
    }
    return switch (priority) {
      case EMERGENCY -> CareTaskPriority.EMERGENCY;
      case HIGH -> CareTaskPriority.HIGH;
      case MEDIUM -> CareTaskPriority.MEDIUM;
      case LOW -> CareTaskPriority.LOW;
    };
  }

  private List<ClinicalEvidence> getSupportingEvidence(ClinicalDecisionSupport cds) {
    if (cds != null && cds.treatmentEvidence() != null) {
      return cds.treatmentEvidence().supportingEvidence();
    }
    return List.of();
  }

  private List<Citation> getSupportingCitations(ClinicalDecisionSupport cds) {
    if (cds != null && cds.diagnosticExplanations() != null) {
      List<Citation> citations = new ArrayList<>();
      for (DiagnosticExplanation exp : cds.diagnosticExplanations()) {
        if (exp.citations() != null) {
          citations.addAll(exp.citations());
        }
      }
      return citations;
    }
    return List.of();
  }

  private Comparator<ClinicalCareTask> getPriorityComparator() {
    return (t1, t2) -> {
      int p1 = getPriorityRank(t1.priority());
      int p2 = getPriorityRank(t2.priority());
      if (p1 != p2) {
        return Integer.compare(p2, p1);
      }
      int a1 = getActorRank(t1.actor());
      int a2 = getActorRank(t2.actor());
      if (a1 != a2) {
        return Integer.compare(a2, a1);
      }
      return t1.title().compareTo(t2.title());
    };
  }

  private int getPriorityRank(CareTaskPriority p) {
    return switch (p) {
      case EMERGENCY -> 4;
      case HIGH -> 3;
      case MEDIUM -> 2;
      case LOW -> 1;
    };
  }

  private int getActorRank(CareTaskActor a) {
    return switch (a) {
      case VETERINARIAN -> 4;
      case REFERRAL_PROVIDER -> 3;
      case LABORATORY -> 2;
      case CAREGIVER, SYSTEM -> 1;
    };
  }
}
