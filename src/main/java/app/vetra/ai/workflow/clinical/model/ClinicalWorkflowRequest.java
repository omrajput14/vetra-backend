package app.vetra.ai.workflow.clinical.model;

import app.vetra.ai.model.AIExecutionContext;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable request triggering the multi-agent Clinical Diagnosis Workflow.
 *
 * @param scanId scan entity identifier
 * @param animalId animal entity identifier
 * @param species target animal species
 * @param breed animal breed
 * @param imageUrl visual input URL for visual pathology inspection
 * @param symptoms observed clinical symptoms
 * @param uploadedBy user UUID who requested the workflow
 * @param executionContext AI execution context
 * @param metadata optional extensible metadata attributes
 */
public record ClinicalWorkflowRequest(
    UUID scanId,
    UUID animalId,
    String species,
    String breed,
    String imageUrl,
    List<String> symptoms,
    UUID uploadedBy,
    AIExecutionContext executionContext,
    Map<String, String> metadata) {

  /** Canonical constructor with non-null defaults. */
  public ClinicalWorkflowRequest {
    scanId = scanId != null ? scanId : UUID.randomUUID();
    animalId = animalId != null ? animalId : UUID.randomUUID();
    species = species != null ? species.trim() : "BOVINE";
    breed = breed != null ? breed.trim() : "Standard";
    imageUrl = imageUrl != null ? imageUrl.trim() : "";
    symptoms = symptoms != null ? List.copyOf(symptoms) : List.of();
    uploadedBy = uploadedBy != null ? uploadedBy : UUID.randomUUID();
    executionContext = executionContext != null ? executionContext : AIExecutionContext.of("default", uploadedBy.toString());
    metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
  }
}
