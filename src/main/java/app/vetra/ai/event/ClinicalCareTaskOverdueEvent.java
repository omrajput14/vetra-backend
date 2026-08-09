package app.vetra.ai.event;

import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskPriority;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskType;
import java.time.Instant;
import java.util.UUID;

/** Domain event published when a clinical care task becomes overdue. */
public record ClinicalCareTaskOverdueEvent(
    UUID caseId, UUID taskId, CareTaskType type, CareTaskPriority priority, Instant timestamp) {}
