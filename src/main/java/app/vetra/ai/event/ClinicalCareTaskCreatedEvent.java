package app.vetra.ai.event;

import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskActor;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskPriority;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskType;
import java.time.Instant;
import java.util.UUID;

/** Domain event published when a new clinical care task is created. */
public record ClinicalCareTaskCreatedEvent(
    UUID caseId, UUID taskId, CareTaskType type, CareTaskPriority priority, CareTaskActor actor, Instant timestamp) {}
