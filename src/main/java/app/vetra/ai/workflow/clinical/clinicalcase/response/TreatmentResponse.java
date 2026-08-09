package app.vetra.ai.workflow.clinical.clinicalcase.response;

import app.vetra.ai.workflow.clinical.model.evidence.ClinicalEvidence;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Immutable analysis result summarizing treatment response between two encounters.
 *
 * @param responseId unique response evaluation identifier
 * @param caseId linked case identifier
 * @param previousEncounterId previous encounter identifier (optional)
 * @param currentEncounterId current encounter identifier
 * @param status determined response classification
 * @param supportingIndicators specific indicators showing improvement
 * @param worseningIndicators specific indicators showing deterioration
 * @param unchangedIndicators specific indicators showing stability
 * @param supportingEvidence underlying clinical evidence items
 * @param assessedAt evaluation timestamp
 */
public record TreatmentResponse(
    UUID responseId,
    UUID caseId,
    UUID previousEncounterId,
    UUID currentEncounterId,
    TreatmentResponseStatus status,
    List<String> supportingIndicators,
    List<String> worseningIndicators,
    List<String> unchangedIndicators,
    List<ClinicalEvidence> supportingEvidence,
    Instant assessedAt) {

  /** Canonical constructor with non-null defaults. */
  public TreatmentResponse {
    responseId = responseId != null ? responseId : UUID.randomUUID();
    caseId = caseId != null ? caseId : UUID.randomUUID();
    currentEncounterId = currentEncounterId != null ? currentEncounterId : UUID.randomUUID();
    status = status != null ? status : TreatmentResponseStatus.INSUFFICIENT_DATA;
    supportingIndicators = supportingIndicators != null ? List.copyOf(supportingIndicators) : List.of();
    worseningIndicators = worseningIndicators != null ? List.copyOf(worseningIndicators) : List.of();
    unchangedIndicators = unchangedIndicators != null ? List.copyOf(unchangedIndicators) : List.of();
    supportingEvidence = supportingEvidence != null ? List.copyOf(supportingEvidence) : List.of();
    assessedAt = assessedAt != null ? assessedAt : Instant.now();
  }
}
