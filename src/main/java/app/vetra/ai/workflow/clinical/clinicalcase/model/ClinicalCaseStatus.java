package app.vetra.ai.workflow.clinical.clinicalcase.model;

/**
 * Longitudinal veterinary clinical case lifecycle status.
 */
public enum ClinicalCaseStatus {
  OPEN,
  UNDER_TREATMENT,
  FOLLOW_UP_REQUIRED,
  REFERRED,
  RESOLVED,
  CLOSED;

  /**
   * Validates state transition rules.
   *
   * @param target target status
   * @return true if transition is valid
   */
  public boolean canTransitionTo(ClinicalCaseStatus target) {
    if (target == null) {
      return false;
    }
    if (this == target) {
      return true;
    }
    return switch (this) {
      case OPEN -> isTransitionFromOpenValid(target);
      case UNDER_TREATMENT -> isTransitionFromUnderTreatmentValid(target);
      case FOLLOW_UP_REQUIRED -> isTransitionFromFollowUpValid(target);
      case REFERRED -> isTransitionFromReferredValid(target);
      case RESOLVED -> target == OPEN || target == CLOSED;
      case CLOSED -> false;
    };
  }

  private boolean isTransitionFromOpenValid(ClinicalCaseStatus target) {
    return target == UNDER_TREATMENT || target == REFERRED || target == CLOSED;
  }

  private boolean isTransitionFromUnderTreatmentValid(ClinicalCaseStatus target) {
    return target == FOLLOW_UP_REQUIRED || target == REFERRED || target == RESOLVED || target == CLOSED;
  }

  private boolean isTransitionFromFollowUpValid(ClinicalCaseStatus target) {
    return target == UNDER_TREATMENT || target == REFERRED || target == RESOLVED || target == CLOSED;
  }

  private boolean isTransitionFromReferredValid(ClinicalCaseStatus target) {
    return target == UNDER_TREATMENT || target == RESOLVED || target == CLOSED;
  }
}
