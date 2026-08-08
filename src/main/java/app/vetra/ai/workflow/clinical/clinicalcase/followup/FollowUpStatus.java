package app.vetra.ai.workflow.clinical.clinicalcase.followup;

/** Operational status of a scheduled clinical follow-up task. */
public enum FollowUpStatus {
  SCHEDULED,
  DUE,
  COMPLETED,
  MISSED,
  CANCELLED,
  ESCALATED
}
