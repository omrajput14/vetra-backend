package app.vetra.ai.event;

import java.time.Instant;
import java.util.UUID;

/** Domain event published when a clinical encounter is attached to a case. */
public record ClinicalEncounterRecordedEvent(UUID caseId, UUID encounterId, UUID scanId, Instant timestamp) {}
