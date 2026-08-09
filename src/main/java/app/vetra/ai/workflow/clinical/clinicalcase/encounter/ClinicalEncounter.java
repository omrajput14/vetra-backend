package app.vetra.ai.workflow.clinical.clinicalcase.encounter;

import app.vetra.ai.workflow.clinical.model.DiseaseCandidate;
import app.vetra.ai.workflow.clinical.model.TreatmentPlan;
import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import app.vetra.ai.workflow.clinical.model.action.ClinicalActionPlan;
import app.vetra.ai.workflow.clinical.model.evidence.MultiModalEvidenceSummary;
import app.vetra.ai.workflow.clinical.model.explainability.ClinicalDecisionSupport;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Immutable historical record of a single clinical interaction within a case.
 *
 * @param encounterId unique encounter identifier
 * @param caseId linked case identifier
 * @param scanId linked diagnostic scan identifier
 * @param occurredAt encounter timestamp
 * @param type encounter classification type
 * @param urgency calculated triage urgency
 * @param primaryDiagnosis primary diagnosis name
 * @param diagnosticConfidence confidence score
 * @param diseaseCandidates ranked disease candidates
 * @param treatmentPlan treatment plan protocol
 * @param actionPlan operational action plan
 * @param decisionSupport decision support & explainability
 * @param evidenceSummary multi-modal evidence breakdown
 */
public record ClinicalEncounter(
    UUID encounterId,
    UUID caseId,
    UUID scanId,
    Instant occurredAt,
    ClinicalEncounterType type,
    TriageUrgency urgency,
    String primaryDiagnosis,
    BigDecimal diagnosticConfidence,
    List<DiseaseCandidate> diseaseCandidates,
    TreatmentPlan treatmentPlan,
    ClinicalActionPlan actionPlan,
    ClinicalDecisionSupport decisionSupport,
    MultiModalEvidenceSummary evidenceSummary) {

  /** Canonical constructor with non-null defaults. */
  public ClinicalEncounter {
    encounterId = encounterId != null ? encounterId : UUID.randomUUID();
    caseId = caseId != null ? caseId : UUID.randomUUID();
    scanId = scanId != null ? scanId : UUID.randomUUID();
    occurredAt = occurredAt != null ? occurredAt : Instant.now();
    type = type != null ? type : ClinicalEncounterType.INITIAL_ASSESSMENT;
    urgency = urgency != null ? urgency : TriageUrgency.ROUTINE;
    primaryDiagnosis = primaryDiagnosis != null ? primaryDiagnosis.trim() : "Unspecified Observation";
    diagnosticConfidence = diagnosticConfidence != null ? diagnosticConfidence : BigDecimal.valueOf(0.10);
    diseaseCandidates = diseaseCandidates != null ? List.copyOf(diseaseCandidates) : List.of();
  }
}
