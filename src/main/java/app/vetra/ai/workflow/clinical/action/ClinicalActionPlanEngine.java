package app.vetra.ai.workflow.clinical.action;

import app.vetra.ai.rag.model.Citation;
import app.vetra.ai.workflow.clinical.model.ClinicalWorkflowContext;
import app.vetra.ai.workflow.clinical.model.TreatmentPlan;
import app.vetra.ai.workflow.clinical.model.TriageAssessment;
import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import app.vetra.ai.workflow.clinical.model.evidence.ClinicalEvidence;
import app.vetra.ai.workflow.clinical.model.action.ActionActor;
import app.vetra.ai.workflow.clinical.model.action.ActionPriority;
import app.vetra.ai.workflow.clinical.model.action.ActionType;
import app.vetra.ai.workflow.clinical.model.action.ClinicalAction;
import app.vetra.ai.workflow.clinical.model.action.ClinicalActionPlan;
import app.vetra.ai.workflow.clinical.model.action.FollowUpPlan;
import app.vetra.ai.workflow.clinical.model.explainability.ClinicalDecisionSupport;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Pure deterministic engine synthesizing auditable {@link ClinicalActionPlan} instances
 * from structured workflow state without invoking AI providers or generating ungrounded facts.
 */
@Component
public class ClinicalActionPlanEngine {

  private static final Logger log = LoggerFactory.getLogger(ClinicalActionPlanEngine.class);
  private static final String ENGINE_VERSION = "1.0.0";

  /**
   * Synthesizes a deterministic {@link ClinicalActionPlan} from context state.
   *
   * @param context workflow execution context
   * @return complete deterministic action plan
   */
  public ClinicalActionPlan synthesizePlan(ClinicalWorkflowContext context) {
    if (context == null) {
      throw new IllegalArgumentException("ClinicalWorkflowContext cannot be null");
    }

    log.debug("ClinicalActionPlanEngine starting deterministic action plan synthesis");

    UUID scanId = context.getRequest().scanId();
    UUID animalId = context.getRequest().animalId();
    TriageAssessment triage = context.getTriageAssessment();
    TreatmentPlan treatment = context.getTreatmentPlan();
    ClinicalDecisionSupport cds = context.getDecisionSupport();

    TriageUrgency urgency = triage != null ? triage.urgency() : TriageUrgency.ROUTINE;
    boolean vetReviewRequired = isVeterinarianReviewRequired(triage, cds);

    List<ClinicalAction> rawActions = new ArrayList<>();

    buildEmergencyAndReferralActions(triage, cds, urgency, vetReviewRequired, rawActions);
    buildTreatmentActions(treatment, context, urgency, rawActions);
    buildIsolationActionsIfSupported(triage, treatment, urgency, rawActions);
    buildMonitoringActions(treatment, triage, urgency, rawActions);
    buildFollowUpActionItem(treatment, urgency, rawActions);

    List<ClinicalAction> deduplicated = deduplicateActions(rawActions);
    List<ClinicalAction> sortedActions = sortActions(deduplicated);

    List<ClinicalAction> immediate = filterByCategorization(sortedActions, "IMMEDIATE");
    List<ClinicalAction> prioritized = filterByCategorization(sortedActions, "PRIORITIZED");
    List<ClinicalAction> monitoring = filterByCategorization(sortedActions, "MONITORING");
    List<ClinicalAction> followUp = filterByCategorization(sortedActions, "FOLLOW_UP");

    FollowUpPlan followUpPlan = buildFollowUpPlan(treatment, triage);
    String escalationSummary = buildEscalationSummary(urgency, vetReviewRequired, triage);
    Map<String, Object> safeAuditMetadata = buildSafeAuditMetadata(context, sortedActions.size());

    log.info(
        "ClinicalActionPlanEngine completed: planActions={}, vetRequired={}",
        sortedActions.size(),
        vetReviewRequired);

    return new ClinicalActionPlan(
        UUID.randomUUID(),
        scanId,
        animalId,
        urgency,
        immediate,
        prioritized,
        monitoring,
        followUp,
        followUpPlan,
        vetReviewRequired,
        escalationSummary,
        Instant.now(),
        safeAuditMetadata);
  }

  private boolean isVeterinarianReviewRequired(TriageAssessment triage, ClinicalDecisionSupport cds) {
    if (cds != null && cds.veterinarianReviewFlag() != null && cds.veterinarianReviewFlag().requiresReview()) {
      return true;
    }
    return triage != null && (triage.requiresImmediateVeterinaryReview() || triage.urgency() == TriageUrgency.EMERGENCY);
  }

  private void buildEmergencyAndReferralActions(
      TriageAssessment triage,
      ClinicalDecisionSupport cds,
      TriageUrgency urgency,
      boolean vetReviewRequired,
      List<ClinicalAction> actions) {

    if (urgency == TriageUrgency.EMERGENCY) {
      String rationale = triage != null ? triage.rationale() : "Immediate emergency clinical triage triggered.";
      actions.add(
          new ClinicalAction(
              UUID.randomUUID().toString(),
              ActionType.VETERINARY_REFERRAL,
              ActionPriority.EMERGENCY,
              ActionActor.VETERINARIAN,
              "Mandatory Emergency Veterinary Referral",
              rationale,
              List.of("Immediate veterinary contact", "Animal stabilization"),
              triage != null ? triage.warningSigns() : List.of(),
              List.of(),
              List.of(),
              true,
              true,
              null,
              "TRIAGE_EMERGENCY_SAFETY_RULE"));

      if (triage != null && triage.recommendedActions() != null) {
        for (String rec : triage.recommendedActions()) {
          actions.add(
              new ClinicalAction(
                  UUID.randomUUID().toString(),
                  ActionType.IMMEDIATE_CARE,
                  ActionPriority.EMERGENCY,
                  ActionActor.FARMER,
                  "Emergency Care: " + rec,
                  rec,
                  List.of(),
                  triage.warningSigns(),
                  List.of(),
                  List.of(),
                  true,
                  false,
                  null,
                  "TRIAGE_RECOMMENDED_ACTION"));
        }
      }
    } else if (vetReviewRequired) {
      ActionPriority priority = (urgency == TriageUrgency.URGENT) ? ActionPriority.HIGH : ActionPriority.MEDIUM;
      List<String> reasons = (cds != null && cds.veterinarianReviewFlag() != null)
          ? cds.veterinarianReviewFlag().reasons()
          : List.of("Veterinarian evaluation required");

      actions.add(
          new ClinicalAction(
              UUID.randomUUID().toString(),
              ActionType.VETERINARY_REFERRAL,
              priority,
              ActionActor.VETERINARIAN,
              "Veterinarian Clinical Review Required",
              "Evaluation required due to: " + String.join("; ", reasons),
              List.of("Schedule veterinary visit"),
              List.of(),
              List.of(),
              List.of(),
              true,
              true,
              null,
              "VETERINARIAN_REVIEW_FLAG"));
    }
  }

  private void buildTreatmentActions(
      TreatmentPlan treatment,
      ClinicalWorkflowContext context,
      TriageUrgency urgency,
      List<ClinicalAction> actions) {

    if (treatment == null) {
      actions.add(
          new ClinicalAction(
              UUID.randomUUID().toString(),
              ActionType.MONITORING,
              ActionPriority.MEDIUM,
              ActionActor.FARMER,
              "Await Treatment Plan",
              "Treatment details unavailable; continue general monitoring until veterinarian review.",
              List.of(),
              List.of("No specific treatment regimen provided"),
              List.of(),
              List.of(),
              false,
              false,
              null,
              "MISSING_TREATMENT_PLAN"));
      return;
    }

    List<Citation> citations = context.getRetrievedContext() != null ? context.getRetrievedContext().citations() : List.of();
    List<ClinicalEvidence> evidence = context.getUnifiedEvidence() != null ? context.getUnifiedEvidence().items() : List.of();
    ActionPriority priority = (urgency == TriageUrgency.EMERGENCY || urgency == TriageUrgency.URGENT)
        ? ActionPriority.HIGH
        : ActionPriority.MEDIUM;

    if (treatment.medications() != null) {
      for (String rx : treatment.medications()) {
        actions.add(
            new ClinicalAction(
                UUID.randomUUID().toString(),
                ActionType.MEDICATION,
                priority,
                ActionActor.FARMER,
                "Administer Medication: " + rx,
                "Administer " + rx + " as specified in treatment regimen.",
                List.of("Verify medication and dosage"),
                treatment.precautions(),
                citations,
                evidence,
                true,
                false,
                null,
                "TREATMENT_PLAN_PRESCRIPTIONS"));
      }
    }

    if (treatment.primaryTreatment() != null && !treatment.primaryTreatment().isBlank()) {
      actions.add(
          new ClinicalAction(
              UUID.randomUUID().toString(),
              ActionType.IMMEDIATE_CARE,
              priority,
              ActionActor.FARMER,
              "Execute Treatment Plan Protocol",
              treatment.primaryTreatment(),
              List.of(),
              treatment.precautions(),
              citations,
              evidence,
              false,
              false,
              null,
              "TREATMENT_PLAN_PROTOCOL"));
    }
  }

  private void buildIsolationActionsIfSupported(
      TriageAssessment triage,
      TreatmentPlan treatment,
      TriageUrgency urgency,
      List<ClinicalAction> actions) {

    String detail = extractIsolationDetail(triage, treatment);
    if (detail != null) {
      ActionPriority priority = (urgency == TriageUrgency.EMERGENCY || urgency == TriageUrgency.URGENT)
          ? ActionPriority.HIGH
          : ActionPriority.MEDIUM;

      actions.add(
          new ClinicalAction(
              UUID.randomUUID().toString(),
              ActionType.ISOLATION,
              priority,
              ActionActor.FARMER,
              "Animal Isolation & Biosecurity Protocol",
              detail,
              List.of("Designate clean quarantine pen"),
              List.of("Prevent shared water and feed troughs"),
              List.of(),
              List.of(),
              true,
              false,
              null,
              "WORKFLOW_BIOSECURITY_WARNING"));
    }
  }

  private String extractIsolationDetail(TriageAssessment triage, TreatmentPlan treatment) {
    if (triage != null) {
      for (String sign : triage.warningSigns()) {
        if (containsIsolationKeyword(sign)) {
          return sign;
        }
      }
      if (triage.recommendedActions() != null) {
        for (String rec : triage.recommendedActions()) {
          if (containsIsolationKeyword(rec)) {
            return rec;
          }
        }
      }
    }
    if (treatment != null && treatment.precautions() != null) {
      for (String prec : treatment.precautions()) {
        if (containsIsolationKeyword(prec)) {
          return prec;
        }
      }
    }
    return null;
  }

  private boolean containsIsolationKeyword(String text) {
    if (text == null) {
      return false;
    }
    String lower = text.toLowerCase();
    return lower.contains("isolate") || lower.contains("quarantine") || lower.contains("biosecurity") || lower.contains("separate");
  }

  private void buildMonitoringActions(
      TreatmentPlan treatment,
      TriageAssessment triage,
      TriageUrgency urgency,
      List<ClinicalAction> actions) {

    List<String> parameters = treatment != null ? treatment.monitoringAdvice() : List.of();
    List<String> warnings = triage != null ? triage.warningSigns() : List.of();

    if (!parameters.isEmpty()) {
      actions.add(
          new ClinicalAction(
              UUID.randomUUID().toString(),
              ActionType.MONITORING,
              ActionPriority.MEDIUM,
              ActionActor.FARMER,
              "Ongoing Clinical Observation & Monitoring",
              "Monitor clinical parameters: " + String.join(", ", parameters),
              List.of("Log daily temp and appetite"),
              warnings,
              List.of(),
              List.of(),
              false,
              false,
              null,
              "TREATMENT_PLAN_MONITORING"));
    }
  }

  private void buildFollowUpActionItem(
      TreatmentPlan treatment,
      TriageUrgency urgency,
      List<ClinicalAction> actions) {

    int days = (treatment != null && treatment.followUpDays() > 0) ? treatment.followUpDays() : 3;
    String interval = days + " days";

    actions.add(
        new ClinicalAction(
            UUID.randomUUID().toString(),
            ActionType.FOLLOW_UP,
            ActionPriority.LOW,
            ActionActor.FARMER,
            "Scheduled Follow-Up Assessment",
            "Re-assess clinical progress in " + interval + ".",
            List.of("Review monitoring log"),
            List.of(),
            List.of(),
            List.of(),
            false,
            false,
            null,
            "SCHEDULED_FOLLOW_UP"));
  }

  private List<ClinicalAction> deduplicateActions(List<ClinicalAction> actions) {
    Map<String, ClinicalAction> map = new LinkedHashMap<>();
    for (ClinicalAction action : actions) {
      String key = action.type().name() + ":" + action.actor().name() + ":" + action.title().trim().toLowerCase();
      if (!map.containsKey(key)) {
        map.put(key, action);
      }
    }
    return new ArrayList<>(map.values());
  }

  private List<ClinicalAction> sortActions(List<ClinicalAction> actions) {
    return actions.stream()
        .sorted(
            Comparator.comparingInt((ClinicalAction a) -> getPriorityRank(a.priority()))
                .thenComparingInt(a -> getTypeRank(a.type()))
                .thenComparing(ClinicalAction::title))
        .toList();
  }

  private int getPriorityRank(ActionPriority priority) {
    return switch (priority) {
      case EMERGENCY -> 0;
      case HIGH -> 1;
      case MEDIUM -> 2;
      case LOW -> 3;
    };
  }

  private int getTypeRank(ActionType type) {
    return switch (type) {
      case VETERINARY_REFERRAL -> 0;
      case IMMEDIATE_CARE -> 1;
      case ISOLATION -> 2;
      case MEDICATION -> 3;
      case MONITORING -> 4;
      case DIAGNOSTIC_TEST -> 5;
      case FOLLOW_UP -> 6;
      case PREVENTIVE_CARE -> 7;
      case OWNER_NOTIFICATION -> 8;
    };
  }

  private List<ClinicalAction> filterByCategorization(List<ClinicalAction> sortedActions, String category) {
    return sortedActions.stream()
        .filter(
            a -> switch (category) {
              case "IMMEDIATE" -> isImmediateAction(a);
              case "PRIORITIZED" -> isPrioritizedAction(a);
              case "MONITORING" -> a.type() == ActionType.MONITORING;
              case "FOLLOW_UP" -> a.type() == ActionType.FOLLOW_UP;
              default -> false;
            })
        .toList();
  }

  private boolean isImmediateAction(ClinicalAction action) {
    return action.priority() == ActionPriority.EMERGENCY
        || (action.priority() == ActionPriority.HIGH
            && (action.type() == ActionType.VETERINARY_REFERRAL
                || action.type() == ActionType.ISOLATION
                || action.type() == ActionType.IMMEDIATE_CARE));
  }

  private boolean isPrioritizedAction(ClinicalAction action) {
    return action.type() == ActionType.MEDICATION
        || action.type() == ActionType.DIAGNOSTIC_TEST
        || action.type() == ActionType.PREVENTIVE_CARE
        || (action.priority() == ActionPriority.HIGH && action.type() != ActionType.VETERINARY_REFERRAL);
  }

  private FollowUpPlan buildFollowUpPlan(TreatmentPlan treatment, TriageAssessment triage) {
    int days = (treatment != null && treatment.followUpDays() > 0) ? treatment.followUpDays() : 3;
    String interval = days + " days";
    List<String> parameters = treatment != null ? treatment.monitoringAdvice() : List.of();
    List<String> escalation = triage != null ? triage.warningSigns() : List.of();

    return new FollowUpPlan("Clinical Re-evaluation", interval, parameters, escalation, ActionActor.FARMER);
  }

  private String buildEscalationSummary(TriageUrgency urgency, boolean vetReviewRequired, TriageAssessment triage) {
    if (urgency == TriageUrgency.EMERGENCY) {
      return "CRITICAL EMERGENCY: Immediate veterinary attendance required. Isolate animal and stabilize.";
    } else if (vetReviewRequired) {
      return "VETERINARIAN REVIEW REQUIRED: Contact veterinarian for diagnostic and prescription confirmation.";
    } else {
      return "ROUTINE CARE: Execute recommended treatment and monitor clinical parameters.";
    }
  }

  private Map<String, Object> buildSafeAuditMetadata(ClinicalWorkflowContext context, int actionCount) {
    Map<String, Object> safeMeta = new HashMap<>();
    safeMeta.put("engineVersion", ENGINE_VERSION);
    safeMeta.put("workflowStepOrder", 8);
    safeMeta.put("actionPlanStrategy", "DETERMINISTIC_WORKFLOW_PROJECTION");
    safeMeta.put("totalGeneratedActions", actionCount);
    safeMeta.put("evaluatedAt", Instant.now().toString());
    safeMeta.put("totalStepCount", 9);
    return safeMeta;
  }
}
