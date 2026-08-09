package app.vetra.ai.workflow.clinical.clinicalcase.operations;

import java.time.Instant;

/**
 * Immutable aggregate operational dashboard summary container.
 */
public record ClinicalOperationsDashboardSummary(
    long totalOpenCases,
    long emergencyCases,
    long veterinarianReviewCases,
    long worseningCases,
    long activeTreatmentCases,
    long followUpRequiredCases,
    long overdueFollowUps,
    long overdueCareTasks,
    long pendingCareTasks,
    long escalatedCareTasks,
    long referredCases,
    long resolvedCases,
    long closedCases,
    Instant nextDueAt,
    long emergencyCount,
    long overdueCount,
    long reviewRequiredCount,
    Instant generatedAt) {

  /** Canonical constructor with non-null defaults. */
  public ClinicalOperationsDashboardSummary {
    generatedAt = generatedAt != null ? generatedAt : Instant.now();
  }
}
