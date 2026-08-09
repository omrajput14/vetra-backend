package app.vetra.ai.workflow.clinical.clinicalcase.followup;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Immutable record representing a scheduled clinical follow-up inspection.
 *
 * @param followUpId unique follow-up identifier
 * @param caseId linked case identifier
 * @param sourceEncounterId encounter ID that originated the follow-up
 * @param scheduledAt target scheduled timestamp
 * @param completedAt actual completion timestamp (optional)
 * @param status current status
 * @param reason reason for follow-up
 * @param expectedObservations clinical parameters to observe
 * @param escalationConditions triggers requiring immediate escalation
 */
public record ClinicalFollowUp(
    UUID followUpId,
    UUID caseId,
    UUID sourceEncounterId,
    Instant scheduledAt,
    Instant completedAt,
    FollowUpStatus status,
    String reason,
    List<String> expectedObservations,
    List<String> escalationConditions) {

  /** Canonical constructor with non-null defaults. */
  public ClinicalFollowUp {
    followUpId = followUpId != null ? followUpId : UUID.randomUUID();
    caseId = caseId != null ? caseId : UUID.randomUUID();
    sourceEncounterId = sourceEncounterId != null ? sourceEncounterId : UUID.randomUUID();
    scheduledAt = scheduledAt != null ? scheduledAt : Instant.now();
    status = status != null ? status : FollowUpStatus.SCHEDULED;
    reason = reason != null ? reason.trim() : "Routine follow-up";
    expectedObservations = expectedObservations != null ? List.copyOf(expectedObservations) : List.of();
    escalationConditions = escalationConditions != null ? List.copyOf(escalationConditions) : List.of();
  }
}
