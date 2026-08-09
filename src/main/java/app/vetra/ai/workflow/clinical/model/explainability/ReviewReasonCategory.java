package app.vetra.ai.workflow.clinical.model.explainability;

/** Categories of deterministic criteria requiring veterinarian review. */
public enum ReviewReasonCategory {
  EMERGENCY_TRIAGE,
  LOW_DIAGNOSTIC_CONFIDENCE,
  INSUFFICIENT_EVIDENCE,
  EVIDENCE_CONFLICT,
  CRITICAL_LAB_OR_VITAL,
  TREATMENT_WARNING
}
