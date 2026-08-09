package app.vetra.ai.workflow.clinical.clinicalcase.operations;

/** Deterministic operational status for clinical case management. */
public enum CaseOperationalStatus {
  EMERGENCY(11),
  VETERINARIAN_REVIEW_REQUIRED(10),
  WORSENING(9),
  FOLLOW_UP_OVERDUE(8),
  CARE_TASK_OVERDUE(7),
  UNDER_TREATMENT(6),
  FOLLOW_UP_REQUIRED(5),
  REFERRED(4),
  STABLE(3),
  RESOLVED(2),
  CLOSED(1);

  private final int precedenceRank;

  CaseOperationalStatus(int precedenceRank) {
    this.precedenceRank = precedenceRank;
  }

  /**
   * Returns numerical precedence rank. Higher rank indicates higher operational urgency.
   *
   * @return precedence rank integer
   */
  public int getPrecedenceRank() {
    return precedenceRank;
  }
}
