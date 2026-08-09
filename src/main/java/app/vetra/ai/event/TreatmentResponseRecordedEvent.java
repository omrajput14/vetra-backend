package app.vetra.ai.event;

import app.vetra.ai.workflow.clinical.clinicalcase.response.TreatmentResponseStatus;
import java.time.Instant;
import java.util.UUID;

/** Domain event published when treatment response progress is evaluated. */
public record TreatmentResponseRecordedEvent(
    UUID caseId, UUID previousEncounterId, UUID currentEncounterId, TreatmentResponseStatus responseStatus, Instant timestamp) {}
