package app.vetra.ai.event;

import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskActor;
import java.time.Instant;
import java.util.UUID;

/** Domain event published when a clinical care task is assigned to an actor. */
public record ClinicalCareTaskAssignedEvent(
    UUID caseId, UUID taskId, CareTaskActor actor, Instant timestamp) {}
