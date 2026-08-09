package app.vetra.ai.event;

import java.time.Instant;
import java.util.UUID;

/** Domain event published when a scheduled clinical follow-up is missed. */
public record ClinicalFollowUpMissedEvent(
    UUID caseId, UUID followUpId, Instant scheduledAt, Instant timestamp) {}
