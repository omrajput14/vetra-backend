package app.vetra.ai.event;

import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import java.time.Instant;
import java.util.UUID;

/** Domain event published when an encounter indicates clinical deterioration or escalation. */
public record ClinicalConditionWorsenedEvent(UUID caseId, UUID encounterId, TriageUrgency urgency, Instant timestamp) {}
