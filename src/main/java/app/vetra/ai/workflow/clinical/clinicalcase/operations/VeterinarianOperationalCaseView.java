package app.vetra.ai.workflow.clinical.clinicalcase.operations;

import app.vetra.ai.workflow.clinical.clinicalcase.encounter.ClinicalEncounter;
import app.vetra.ai.workflow.clinical.clinicalcase.model.ClinicalCaseStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.response.TreatmentResponse;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.ClinicalCareTask;
import app.vetra.ai.workflow.clinical.clinicalcase.timeline.ClinicalCaseTimeline;
import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import app.vetra.ai.workflow.clinical.model.explainability.ClinicalDecisionSupport;
import app.vetra.ai.workflow.clinical.model.explainability.VeterinarianReviewFlag;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable operational read-model projection tailored for clinical staff and veterinarians.
 */
public record VeterinarianOperationalCaseView(
    UUID caseId,
    UUID animalId,
    String species,
    String breed,
    String primaryCondition,
    ClinicalCaseStatus currentCaseStatus,
    CaseOperationalStatus operationalStatus,
    ClinicalEncounter latestEncounter,
    String latestDiagnosis,
    BigDecimal diagnosticConfidence,
    TriageUrgency triageUrgency,
    TreatmentResponse treatmentResponse,
    VeterinarianReviewFlag veterinarianReviewFlag,
    boolean hasEvidenceConflicts,
    String uncertaintyLevel,
    ClinicalDecisionSupport decisionSupport,
    List<ClinicalCareTask> activeCareTasks,
    List<ClinicalCareTask> overdueTasks,
    ClinicalCaseTimeline timelineSummary,
    String nextOperationalAction,
    Instant lastUpdatedAt) {

  /** Canonical constructor with non-null defaults. */
  public VeterinarianOperationalCaseView {
    caseId = Objects.requireNonNullElseGet(caseId, UUID::randomUUID);
    animalId = Objects.requireNonNullElseGet(animalId, UUID::randomUUID);
    species = species != null ? species.trim() : "UNKNOWN";
    breed = breed != null ? breed.trim() : "UNKNOWN";
    primaryCondition = primaryCondition != null ? primaryCondition.trim() : "UNKNOWN";
    currentCaseStatus = Objects.requireNonNullElse(currentCaseStatus, ClinicalCaseStatus.OPEN);
    operationalStatus = Objects.requireNonNullElse(operationalStatus, CaseOperationalStatus.STABLE);
    latestDiagnosis = latestDiagnosis != null ? latestDiagnosis.trim() : primaryCondition;
    diagnosticConfidence = Objects.requireNonNullElse(diagnosticConfidence, BigDecimal.ZERO);
    triageUrgency = Objects.requireNonNullElse(triageUrgency, TriageUrgency.ROUTINE);
    uncertaintyLevel = uncertaintyLevel != null ? uncertaintyLevel.trim() : "LOW";
    activeCareTasks = activeCareTasks != null ? List.copyOf(activeCareTasks) : List.of();
    overdueTasks = overdueTasks != null ? List.copyOf(overdueTasks) : List.of();
    nextOperationalAction = nextOperationalAction != null ? nextOperationalAction.trim() : "";
    lastUpdatedAt = Objects.requireNonNullElseGet(lastUpdatedAt, Instant::now);
  }
}
