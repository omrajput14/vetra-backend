package app.vetra.ai.event;

import java.time.Instant;
import java.util.UUID;

/** Domain event published when a new longitudinal clinical case is created. */
public record ClinicalCaseCreatedEvent(UUID caseId, UUID animalId, Instant timestamp) {}
