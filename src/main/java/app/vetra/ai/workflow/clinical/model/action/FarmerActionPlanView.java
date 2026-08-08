package app.vetra.ai.workflow.clinical.model.action;

import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import java.util.ArrayList;
import java.util.List;

/**
 * Farmer/Caregiver presentation projection focusing on clear immediate steps, biosecurity,
 * monitoring instructions, and escalation conditions without technical metadata.
 *
 * @param planId plan identifier
 * @param urgency triage urgency classification
 * @param veterinarianReferralRequired true if veterinarian attendance is needed
 * @param immediateSteps concise list of immediate care steps for the farmer
 * @param biosecurityWarnings biosecurity precautions and quarantine warnings
 * @param signsToMonitor parameters to observe on the farm
 * @param escalationConditions triggers requiring immediate veterinary re-contact
 * @param summary summary escalation guidance
 */
public record FarmerActionPlanView(
    String planId,
    TriageUrgency urgency,
    boolean veterinarianReferralRequired,
    List<String> immediateSteps,
    List<String> biosecurityWarnings,
    List<String> signsToMonitor,
    List<String> escalationConditions,
    String summary) {

  /**
   * Projects a {@link ClinicalActionPlan} into a farmer-friendly presentation view.
   *
   * @param plan canonical action plan
   * @return farmer action plan view
   */
  public static FarmerActionPlanView fromActionPlan(ClinicalActionPlan plan) {
    if (plan == null) {
      return new FarmerActionPlanView(
          "", TriageUrgency.ROUTINE, false, List.of(), List.of(), List.of(), List.of(), "No action plan available.");
    }

    List<String> immediateSteps = new ArrayList<>();
    List<String> biosecurity = new ArrayList<>();

    for (ClinicalAction action : plan.immediateActions()) {
      if (action.actor() == ActionActor.FARMER || action.actor() == ActionActor.CAREGIVER) {
        immediateSteps.add(action.title() + ": " + action.description());
      }
      if (action.type() == ActionType.ISOLATION || !action.warnings().isEmpty()) {
        biosecurity.addAll(action.warnings());
      }
    }
    for (ClinicalAction action : plan.prioritizedActions()) {
      if (action.actor() == ActionActor.FARMER || action.actor() == ActionActor.CAREGIVER) {
        immediateSteps.add(action.title() + ": " + action.description());
      }
    }

    List<String> monitoring = plan.followUpPlan() != null ? plan.followUpPlan().monitoringParameters() : List.of();
    List<String> escalation = plan.followUpPlan() != null ? plan.followUpPlan().escalationConditions() : List.of();

    return new FarmerActionPlanView(
        plan.planId().toString(),
        plan.urgency(),
        plan.veterinarianReviewRequired(),
        immediateSteps,
        biosecurity,
        monitoring,
        escalation,
        plan.escalationSummary());
  }
}
