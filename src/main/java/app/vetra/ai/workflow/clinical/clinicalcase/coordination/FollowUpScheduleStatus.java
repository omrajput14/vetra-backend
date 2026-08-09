package app.vetra.ai.workflow.clinical.clinicalcase.coordination;

/** Operational status for a scheduled follow-up inspection. */
public enum FollowUpScheduleStatus {
  SCHEDULED,
  DUE,
  OVERDUE,
  COMPLETED,
  MISSED,
  CANCELLED,
  ESCALATED
}
