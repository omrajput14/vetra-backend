package app.vetra.ai.workflow.clinical.clinicalcase.operations;

/** Deterministic queue classification for case work queue inclusion. */
public enum CaseWorkQueueReason {
  EMERGENCY,
  VETERINARIAN_REVIEW,
  WORSENING_RESPONSE,
  OVERDUE_CARE_TASK,
  OVERDUE_FOLLOW_UP,
  DUE_FOLLOW_UP,
  ACTIVE_TREATMENT,
  ROUTINE_CASE_REVIEW
}
