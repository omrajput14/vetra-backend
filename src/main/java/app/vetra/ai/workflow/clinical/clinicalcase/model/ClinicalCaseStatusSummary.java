package app.vetra.ai.workflow.clinical.clinicalcase.model;

import app.vetra.ai.workflow.clinical.clinicalcase.response.TreatmentResponseStatus;
import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Deterministic status summary projection for a longitudinal clinical case.
 *
 * @param caseId case identifier
 * @param currentStatus current lifecycle status
 * @param latestUrgency latest calculated triage urgency
 * @param latestDiagnosis latest primary diagnosis
 * @param latestConfidence latest diagnostic confidence score
 * @param treatmentResponseStatus latest treatment response classification
 * @param veterinarianReviewRequired true if veterinarian review is required
 * @param openFollowUps count of open scheduled follow-ups
 * @param lastEncounterAt timestamp of latest encounter
 * @param nextFollowUpAt timestamp of next scheduled follow-up
 */
public record ClinicalCaseStatusSummary(
    UUID caseId,
    ClinicalCaseStatus currentStatus,
    TriageUrgency latestUrgency,
    String latestDiagnosis,
    BigDecimal latestConfidence,
    TreatmentResponseStatus treatmentResponseStatus,
    boolean veterinarianReviewRequired,
    int openFollowUps,
    Instant lastEncounterAt,
    Instant nextFollowUpAt) {

  /** Canonical constructor with non-null defaults. */
  public ClinicalCaseStatusSummary {
    caseId = caseId != null ? caseId : UUID.randomUUID();
    currentStatus = currentStatus != null ? currentStatus : ClinicalCaseStatus.OPEN;
    latestUrgency = latestUrgency != null ? latestUrgency : TriageUrgency.ROUTINE;
    latestDiagnosis = latestDiagnosis != null ? latestDiagnosis.trim() : "Unspecified Observation";
    latestConfidence = latestConfidence != null ? latestConfidence : BigDecimal.valueOf(0.10);
    treatmentResponseStatus = treatmentResponseStatus != null ? treatmentResponseStatus : TreatmentResponseStatus.INSUFFICIENT_DATA;
  }
}
