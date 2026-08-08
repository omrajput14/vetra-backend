package app.vetra.ai.workflow.clinical.model.explainability;

import app.vetra.ai.rag.model.Citation;
import app.vetra.ai.workflow.clinical.model.evidence.ClinicalEvidence;
import java.util.List;

/**
 * Provenance record linking treatment recommendations to target conditions, evidence, and citations.
 *
 * @param targetedDiseases list of candidate disease names targeted by treatment
 * @param supportingEvidence list of clinical evidence items justifying treatment
 * @param supportingCitations list of literature citations supporting treatment
 * @param warningsAndContraindications list of species/condition precautions and contraindications
 */
public record TreatmentEvidence(
    List<String> targetedDiseases,
    List<ClinicalEvidence> supportingEvidence,
    List<Citation> supportingCitations,
    List<String> warningsAndContraindications) {

  /** Canonical constructor with non-null defaults. */
  public TreatmentEvidence {
    targetedDiseases = targetedDiseases != null ? List.copyOf(targetedDiseases) : List.of();
    supportingEvidence = supportingEvidence != null ? List.copyOf(supportingEvidence) : List.of();
    supportingCitations = supportingCitations != null ? List.copyOf(supportingCitations) : List.of();
    warningsAndContraindications =
        warningsAndContraindications != null ? List.copyOf(warningsAndContraindications) : List.of();
  }
}
