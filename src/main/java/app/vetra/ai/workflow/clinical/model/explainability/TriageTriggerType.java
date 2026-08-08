package app.vetra.ai.workflow.clinical.model.explainability;

/** Classification of the source that triggered the triage urgency assessment. */
public enum TriageTriggerType {
  DETERMINISTIC_SAFETY_RULE,
  AI_ASSESSMENT
}
