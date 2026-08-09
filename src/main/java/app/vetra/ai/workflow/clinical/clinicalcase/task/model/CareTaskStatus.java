package app.vetra.ai.workflow.clinical.clinicalcase.task.model;

/** Lifecycle state for clinical care tasks with strict deterministic transition rules. */
public enum CareTaskStatus {
  PENDING,
  ASSIGNED,
  IN_PROGRESS,
  DUE,
  OVERDUE,
  COMPLETED,
  CANCELLED,
  ESCALATED;

  /**
   * Validates deterministic task lifecycle transitions.
   *
   * @param target target status
   * @return true if transition is valid
   */
  public boolean canTransitionTo(CareTaskStatus target) {
    if (target == null) {
      return false;
    }
    if (this == target) {
      return true;
    }
    return switch (this) {
      case PENDING -> isTransitionFromPendingValid(target);
      case ASSIGNED -> isTransitionFromAssignedValid(target);
      case IN_PROGRESS -> isTransitionFromActiveValid(target);
      case DUE -> isTransitionFromActiveValid(target);
      case OVERDUE -> isTransitionFromActiveValid(target);
      case COMPLETED, CANCELLED, ESCALATED -> false;
    };
  }

  private boolean isTransitionFromPendingValid(CareTaskStatus target) {
    return target == ASSIGNED || target == IN_PROGRESS || target == DUE || target == OVERDUE || target == COMPLETED || target == CANCELLED || target == ESCALATED;
  }

  private boolean isTransitionFromAssignedValid(CareTaskStatus target) {
    return target == IN_PROGRESS || target == DUE || target == OVERDUE || target == COMPLETED || target == CANCELLED || target == ESCALATED;
  }

  private boolean isTransitionFromActiveValid(CareTaskStatus target) {
    return target == DUE || target == OVERDUE || target == COMPLETED || target == CANCELLED || target == ESCALATED;
  }
}
