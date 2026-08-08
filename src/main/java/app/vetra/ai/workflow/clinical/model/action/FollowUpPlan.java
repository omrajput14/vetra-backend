package app.vetra.ai.workflow.clinical.model.action;

import java.util.List;

/**
 * Structured follow-up schedule and escalation triggers derived from treatment/triage state.
 *
 * @param followUpType type of follow-up required
 * @param recommendedInterval interval (e.g. "3 days", "24 hours")
 * @param monitoringParameters parameters to observe during follow-up
 * @param escalationConditions conditions that trigger immediate escalation
 * @param responsibleActor actor responsible for follow-up
 */
public record FollowUpPlan(
    String followUpType,
    String recommendedInterval,
    List<String> monitoringParameters,
    List<String> escalationConditions,
    ActionActor responsibleActor) {

  /** Canonical constructor with non-null defaults. */
  public FollowUpPlan {
    followUpType = followUpType != null ? followUpType.trim() : "Standard Follow-Up";
    recommendedInterval = recommendedInterval != null ? recommendedInterval.trim() : "As needed";
    monitoringParameters = monitoringParameters != null ? List.copyOf(monitoringParameters) : List.of();
    escalationConditions = escalationConditions != null ? List.copyOf(escalationConditions) : List.of();
    responsibleActor = responsibleActor != null ? responsibleActor : ActionActor.FARMER;
  }
}
