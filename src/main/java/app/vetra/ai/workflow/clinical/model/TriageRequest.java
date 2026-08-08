package app.vetra.ai.workflow.clinical.model;

import app.vetra.ai.model.AIExecutionContext;
import app.vetra.ai.workflow.clinical.model.evidence.UnifiedClinicalEvidence;
import java.util.List;
import java.util.Map;

/**
 * Dedicated request model carrying clinical findings required for triage evaluation.
 *
 * @param species target animal species
 * @param breed animal breed
 * @param symptoms observed clinical symptoms
 * @param diagnosisObservations visual pathology observations from DiagnosisStep
 * @param rankedDiseases candidate diseases from RankingStep
 * @param retrievedEvidence literature context from KnowledgeStep (RAG)
 * @param unifiedEvidence unified multi-modal evidence collection (optional)
 * @param metadata optional context metadata
 * @param executionContext AI execution context
 */
public record TriageRequest(
    String species,
    String breed,
    List<String> symptoms,
    List<String> diagnosisObservations,
    List<DiseaseCandidate> rankedDiseases,
    String retrievedEvidence,
    UnifiedClinicalEvidence unifiedEvidence,
    Map<String, String> metadata,
    AIExecutionContext executionContext) {

  /** Backward-compatible 8-argument constructor. */
  public TriageRequest(
      String species,
      String breed,
      List<String> symptoms,
      List<String> diagnosisObservations,
      List<DiseaseCandidate> rankedDiseases,
      String retrievedEvidence,
      Map<String, String> metadata,
      AIExecutionContext executionContext) {
    this(
        species,
        breed,
        symptoms,
        diagnosisObservations,
        rankedDiseases,
        retrievedEvidence,
        null,
        metadata,
        executionContext);
  }

  /** Canonical constructor with non-null defaults. */
  public TriageRequest {
    species = species != null ? species.trim() : "BOVINE";
    breed = breed != null ? breed.trim() : "Standard";
    symptoms = symptoms != null ? List.copyOf(symptoms) : List.of();
    diagnosisObservations = diagnosisObservations != null ? List.copyOf(diagnosisObservations) : List.of();
    rankedDiseases = rankedDiseases != null ? List.copyOf(rankedDiseases) : List.of();
    retrievedEvidence = retrievedEvidence != null ? retrievedEvidence.trim() : "";
    metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
  }
}
