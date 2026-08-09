package app.vetra.ai.workflow.clinical.clinicalcase.task.model;

/** Operational classification of a clinical care task. */
public enum CareTaskType {
  VETERINARIAN_REVIEW,
  FOLLOW_UP,
  MONITORING,
  DIAGNOSTIC_TEST,
  TREATMENT_REVIEW,
  OWNER_CONTACT,
  REFERRAL,
  EMERGENCY_ESCALATION,
  CASE_REVIEW
}
