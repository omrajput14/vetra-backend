package app.vetra.ai.event;

import java.time.Instant;
import java.util.UUID;

/** Domain event published when a clinical case is resolved or closed. */
public record ClinicalCaseResolvedEvent(UUID caseId, Instant timestamp) {}
