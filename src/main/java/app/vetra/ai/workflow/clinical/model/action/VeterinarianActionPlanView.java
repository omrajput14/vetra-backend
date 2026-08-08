package app.vetra.ai.workflow.clinical.model.action;

import java.util.ArrayList;
import java.util.List;

/**
 * Veterinarian presentation projection exposing full action details, evidence provenance,
 * citations, treatment warnings, diagnostic uncertainty, conflicts, and review reasons.
 *
 * @param fullPlan full canonical action plan
 * @param allActions complete ordered list of actions
 * @param veterinarianReviewRequired true if vet review flag is set
 * @param totalActionCount total number of generated actions
 */
public record VeterinarianActionPlanView(
    ClinicalActionPlan fullPlan,
    List<ClinicalAction> allActions,
    boolean veterinarianReviewRequired,
    int totalActionCount) {

  /**
   * Projects a {@link ClinicalActionPlan} into a veterinarian-focused complete presentation view.
   *
   * @param plan canonical action plan
   * @return veterinarian action plan view
   */
  public static VeterinarianActionPlanView fromActionPlan(ClinicalActionPlan plan) {
    if (plan == null) {
      return new VeterinarianActionPlanView(null, List.of(), false, 0);
    }

    List<ClinicalAction> combined = new ArrayList<>();
    combined.addAll(plan.immediateActions());
    combined.addAll(plan.prioritizedActions());
    combined.addAll(plan.monitoringActions());
    combined.addAll(plan.followUpActions());

    return new VeterinarianActionPlanView(plan, combined, plan.veterinarianReviewRequired(), combined.size());
  }
}
