package app.vetra.ai.workflow.clinical.model.explainability;

import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import java.util.List;

/**
 * Explains the assigned triage urgency level and identifies trigger classification.
 *
 * @param assignedUrgency assigned triage urgency level
 * @param triggerType trigger source classification (DETERMINISTIC_SAFETY_RULE vs AI_ASSESSMENT)
 * @param triggeredRules list of triggered deterministic rule names/descriptions
 * @param contributingFactors key clinical symptoms/indicators driving the assessment
 * @param escalationRationale explicit safety precedence rationale
 */
public record TriageExplanation(
    TriageUrgency assignedUrgency,
    TriageTriggerType triggerType,
    List<String> triggeredRules,
    List<String> contributingFactors,
    String escalationRationale) {

  /** Canonical constructor with non-null defaults. */
  public TriageExplanation {
    assignedUrgency = assignedUrgency != null ? assignedUrgency : TriageUrgency.ROUTINE;
    triggerType = triggerType != null ? triggerType : TriageTriggerType.AI_ASSESSMENT;
    triggeredRules = triggeredRules != null ? List.copyOf(triggeredRules) : List.of();
    contributingFactors = contributingFactors != null ? List.copyOf(contributingFactors) : List.of();
    escalationRationale = escalationRationale != null ? escalationRationale.trim() : "";
  }
}
