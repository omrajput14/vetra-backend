package app.vetra.ai.event;

import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskPriority;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskType;
import java.time.Instant;
import java.util.UUID;

/** Domain event published when a clinical care task is escalated. */
public record ClinicalCareTaskEscalatedEvent(
    UUID caseId, UUID taskId, CareTaskType type, CareTaskPriority priority, String reason, Instant timestamp) {}
