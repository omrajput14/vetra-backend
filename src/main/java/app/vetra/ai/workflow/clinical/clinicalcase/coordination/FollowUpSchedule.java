package app.vetra.ai.workflow.clinical.clinicalcase.coordination;

import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskActor;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Immutable schedule record derived from existing clinical follow-up data.
 *
 * @param scheduleId unique schedule identifier
 * @param caseId linked case identifier
 * @param sourceEncounterId originating encounter ID
 * @param followUpId originating follow-up ID
 * @param scheduledAt target scheduled timestamp
 * @param status current schedule status
 * @param reason clinical reason for follow-up
 * @param expectedObservations clinical parameters to observe
 * @param escalationConditions triggers requiring immediate escalation
 * @param responsibleActor target responsible actor
 * @param createdAt schedule creation timestamp
 */
public record FollowUpSchedule(
    UUID scheduleId,
    UUID caseId,
    UUID sourceEncounterId,
    UUID followUpId,
    Instant scheduledAt,
    FollowUpScheduleStatus status,
    String reason,
    List<String> expectedObservations,
    List<String> escalationConditions,
    CareTaskActor responsibleActor,
    Instant createdAt) {

  /** Canonical constructor with non-null defaults. */
  public FollowUpSchedule {
    scheduleId = scheduleId != null ? scheduleId : UUID.randomUUID();
    caseId = caseId != null ? caseId : UUID.randomUUID();
    sourceEncounterId = sourceEncounterId != null ? sourceEncounterId : UUID.randomUUID();
    followUpId = followUpId != null ? followUpId : UUID.randomUUID();
    scheduledAt = scheduledAt != null ? scheduledAt : Instant.now();
    status = status != null ? status : FollowUpScheduleStatus.SCHEDULED;
    reason = reason != null ? reason.trim() : "Routine follow-up inspection";
    expectedObservations = expectedObservations != null ? List.copyOf(expectedObservations) : List.of();
    escalationConditions = escalationConditions != null ? List.copyOf(escalationConditions) : List.of();
    responsibleActor = responsibleActor != null ? responsibleActor : CareTaskActor.CAREGIVER;
    createdAt = createdAt != null ? createdAt : Instant.now();
  }
}
