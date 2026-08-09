package app.vetra.ai.workflow.clinical.clinicalcase.task.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable operational summary projection of care task status metrics for a case.
 *
 * @param caseId linked case identifier
 * @param totalTasks total care task count
 * @param pendingTasks pending task count
 * @param dueTasks due task count
 * @param overdueTasks overdue task count
 * @param completedTasks completed task count
 * @param escalatedTasks escalated task count
 * @param emergencyTasks emergency priority task count
 * @param veterinarianTasks veterinarian assigned task count
 * @param nextDueAt timestamp of next scheduled task due
 */
public record CareTaskSummary(
    UUID caseId,
    int totalTasks,
    int pendingTasks,
    int dueTasks,
    int overdueTasks,
    int completedTasks,
    int escalatedTasks,
    int emergencyTasks,
    int veterinarianTasks,
    Instant nextDueAt) {

  /** Canonical constructor with non-null defaults. */
  public CareTaskSummary {
    caseId = caseId != null ? caseId : UUID.randomUUID();
  }
}
