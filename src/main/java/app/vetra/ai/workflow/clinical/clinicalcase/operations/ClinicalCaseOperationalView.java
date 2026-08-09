package app.vetra.ai.workflow.clinical.clinicalcase.operations;

import app.vetra.ai.workflow.clinical.clinicalcase.model.ClinicalCaseStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.response.TreatmentResponseStatus;
import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable read-model projection of a clinical case for operational management.
 *
 * <p>Maps existing canonical state without inventing clinical conclusions or facts.
 */
public record ClinicalCaseOperationalView(
    UUID caseId,
    UUID animalId,
    String species,
    String breed,
    String primaryCondition,
    ClinicalCaseStatus currentCaseStatus,
    CaseOperationalStatus operationalStatus,
    UUID latestEncounterId,
    Instant latestEncounterAt,
    TriageUrgency latestUrgency,
    String latestDiagnosis,
    BigDecimal latestDiagnosticConfidence,
    TreatmentResponseStatus treatmentResponseStatus,
    boolean veterinarianReviewRequired,
    boolean emergency,
    int openTaskCount,
    int overdueTaskCount,
    int emergencyTaskCount,
    int pendingFollowUpCount,
    int overdueFollowUpCount,
    Instant nextDueAt,
    String nextOperationalAction,
    Instant lastUpdatedAt) {

  /** Canonical constructor with non-null defaults. */
  public ClinicalCaseOperationalView {
    caseId = caseId != null ? caseId : UUID.randomUUID();
    animalId = animalId != null ? animalId : UUID.randomUUID();
    species = species != null ? species.trim() : "UNKNOWN";
    breed = breed != null ? breed.trim() : "UNKNOWN";
    primaryCondition = primaryCondition != null ? primaryCondition.trim() : "UNKNOWN";
    currentCaseStatus = currentCaseStatus != null ? currentCaseStatus : ClinicalCaseStatus.OPEN;
    operationalStatus = operationalStatus != null ? operationalStatus : CaseOperationalStatus.STABLE;
    latestUrgency = latestUrgency != null ? latestUrgency : TriageUrgency.ROUTINE;
    latestDiagnosis = latestDiagnosis != null ? latestDiagnosis.trim() : primaryCondition;
    latestDiagnosticConfidence = latestDiagnosticConfidence != null ? latestDiagnosticConfidence : BigDecimal.ZERO;
    treatmentResponseStatus = treatmentResponseStatus != null ? treatmentResponseStatus : TreatmentResponseStatus.INSUFFICIENT_DATA;
    nextOperationalAction = nextOperationalAction != null ? nextOperationalAction.trim() : "";
    lastUpdatedAt = lastUpdatedAt != null ? lastUpdatedAt : Instant.now();
  }
}
