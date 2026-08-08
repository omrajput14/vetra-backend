package app.vetra.ai.workflow.clinical.model.explainability;

import app.vetra.ai.rag.model.Citation;
import app.vetra.ai.workflow.clinical.model.evidence.ClinicalEvidence;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Deterministic evidence-to-diagnosis traceability record for a specific disease candidate.
 *
 * @param diseaseName target candidate disease name
 * @param supportingEvidence list of matched supporting evidence items
 * @param contradictoryEvidence list of conflicting evidence items
 * @param citations list of grounded literature citations
 * @param modalityContributions normalized contribution weights by modality
 * @param confidence top candidate confidence score
 * @param uncertaintyLevel calculated uncertainty tier
 * @param uncertaintyIndicators specific rationale for diagnostic uncertainty
 */
public record DiagnosticExplanation(
    String diseaseName,
    List<ClinicalEvidence> supportingEvidence,
    List<ClinicalEvidence> contradictoryEvidence,
    List<Citation> citations,
    Map<String, Double> modalityContributions,
    BigDecimal confidence,
    UncertaintyLevel uncertaintyLevel,
    List<String> uncertaintyIndicators) {

  /** Canonical constructor with non-null defaults. */
  public DiagnosticExplanation {
    diseaseName = diseaseName != null ? diseaseName.trim() : "Unknown Condition";
    supportingEvidence = supportingEvidence != null ? List.copyOf(supportingEvidence) : List.of();
    contradictoryEvidence = contradictoryEvidence != null ? List.copyOf(contradictoryEvidence) : List.of();
    citations = citations != null ? List.copyOf(citations) : List.of();
    modalityContributions = modalityContributions != null ? Map.copyOf(modalityContributions) : Map.of();
    confidence = confidence != null ? confidence : BigDecimal.ZERO;
    uncertaintyLevel = uncertaintyLevel != null ? uncertaintyLevel : UncertaintyLevel.INSUFFICIENT_EVIDENCE;
    uncertaintyIndicators = uncertaintyIndicators != null ? List.copyOf(uncertaintyIndicators) : List.of();
  }
}
