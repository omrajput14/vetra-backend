package app.vetra.ai.workflow.clinical.model.explainability;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Master Clinical Decision Support & Explainability container produced by ClinicalDecisionSupportEngine.
 *
 * @param primaryConclusion synthesized high-level clinical summary
 * @param diagnosticExplanations list of per-candidate diagnostic evidence traceability records
 * @param triageExplanation detailed explainability breakdown for triage urgency assignment
 * @param treatmentEvidence provenance linking treatment recommendations to evidence and citations
 * @param uncertaintyAssessment structured quantification of clinical uncertainty
 * @param contradictoryEvidenceSummary genuine evidence conflicts and literature discrepancies
 * @param veterinarianReviewFlag deterministic criteria for requiring professional vet review
 * @param auditMetadata safe structural audit metadata (engine version, step order, timestamp)
 * @param timestamp creation timestamp
 */
public record ClinicalDecisionSupport(
    String primaryConclusion,
    List<DiagnosticExplanation> diagnosticExplanations,
    TriageExplanation triageExplanation,
    TreatmentEvidence treatmentEvidence,
    ClinicalUncertainty uncertaintyAssessment,
    ContradictoryEvidenceSummary contradictoryEvidenceSummary,
    VeterinarianReviewFlag veterinarianReviewFlag,
    Map<String, Object> auditMetadata,
    Instant timestamp) {

  /** Canonical constructor with non-null defaults. */
  public ClinicalDecisionSupport {
    primaryConclusion = primaryConclusion != null ? primaryConclusion.trim() : "";
    diagnosticExplanations = diagnosticExplanations != null ? List.copyOf(diagnosticExplanations) : List.of();
    triageExplanation = triageExplanation != null ? triageExplanation : new TriageExplanation(null, null, null, null, null);
    treatmentEvidence = treatmentEvidence != null ? treatmentEvidence : new TreatmentEvidence(null, null, null, null);
    uncertaintyAssessment = uncertaintyAssessment != null ? uncertaintyAssessment : new ClinicalUncertainty(null, null, null, null);
    contradictoryEvidenceSummary = contradictoryEvidenceSummary != null ? contradictoryEvidenceSummary : new ContradictoryEvidenceSummary(null, null, null);
    veterinarianReviewFlag = veterinarianReviewFlag != null ? veterinarianReviewFlag : new VeterinarianReviewFlag(false, null, null);
    auditMetadata = auditMetadata != null ? Map.copyOf(auditMetadata) : Map.of();
    timestamp = timestamp != null ? timestamp : Instant.now();
  }
}
