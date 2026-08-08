package app.vetra.ai.workflow.clinical.model;

import app.vetra.ai.model.AIExecutionContext;
import java.math.BigDecimal;
import java.util.List;

/**
 * Dedicated request model passed to TreatmentAgent, ensuring complete decoupling from
 * internal DiseaseRanker structures.
 *
 * @param primaryCondition top ranked disease or syndrome name
 * @param confidence confidence level of the diagnosis
 * @param species target animal species
 * @param breed animal breed
 * @param symptoms observed clinical symptoms
 * @param supportingEvidence clinical evidence summary
 * @param executionContext AI execution context
 */
public record TreatmentRequest(
    String primaryCondition,
    BigDecimal confidence,
    String species,
    String breed,
    List<String> symptoms,
    String supportingEvidence,
    AIExecutionContext executionContext) {

  /** Canonical constructor with non-null defaults. */
  public TreatmentRequest {
    primaryCondition = primaryCondition != null ? primaryCondition.trim() : "Unspecified Observation";
    confidence = confidence != null ? confidence : BigDecimal.valueOf(0.50);
    species = species != null ? species.trim() : "BOVINE";
    breed = breed != null ? breed.trim() : "Standard";
    symptoms = symptoms != null ? List.copyOf(symptoms) : List.of();
    supportingEvidence = supportingEvidence != null ? supportingEvidence.trim() : "";
  }
}
