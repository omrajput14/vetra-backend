package app.vetra.ai.workflow.clinical.model;

import app.vetra.ai.model.AIExecutionContext;
import app.vetra.ai.workflow.clinical.model.evidence.ClinicalHistory;
import app.vetra.ai.workflow.clinical.model.evidence.LaboratoryResult;
import app.vetra.ai.workflow.clinical.model.evidence.SensorObservation;
import app.vetra.ai.workflow.clinical.model.evidence.VitalSign;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable request triggering the multi-agent Clinical Diagnosis Workflow with multi-modal evidence inputs.
 *
 * @param scanId scan entity identifier
 * @param animalId animal entity identifier
 * @param species target animal species
 * @param breed animal breed
 * @param imageUrl visual input URL for visual pathology inspection
 * @param symptoms observed clinical symptoms
 * @param labResults optional laboratory test results
 * @param vitalSigns optional physiological vital signs
 * @param sensorObservations optional IoT sensor telemetry
 * @param clinicalHistory optional medical history records
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
    List<LaboratoryResult> labResults,
    List<VitalSign> vitalSigns,
    List<SensorObservation> sensorObservations,
    List<ClinicalHistory> clinicalHistory,
    UUID uploadedBy,
    AIExecutionContext executionContext,
    Map<String, String> metadata) {

  /** Backward-compatible 9-argument constructor. */
  @SuppressWarnings("checkstyle:ParameterNumber")
  public ClinicalWorkflowRequest(
      UUID scanId,
      UUID animalId,
      String species,
      String breed,
      String imageUrl,
      List<String> symptoms,
      UUID uploadedBy,
      AIExecutionContext executionContext,
      Map<String, String> metadata) {
    this(
        scanId,
        animalId,
        species,
        breed,
        imageUrl,
        symptoms,
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        uploadedBy,
        executionContext,
        metadata);
  }

  /** Canonical constructor with non-null defaults. */
  @SuppressWarnings("checkstyle:ParameterNumber")
  public ClinicalWorkflowRequest {
    scanId = scanId != null ? scanId : UUID.randomUUID();
    animalId = animalId != null ? animalId : UUID.randomUUID();
    species = species != null ? species.trim() : "BOVINE";
    breed = breed != null ? breed.trim() : "Standard";
    imageUrl = imageUrl != null ? imageUrl.trim() : "";
    symptoms = symptoms != null ? List.copyOf(symptoms) : List.of();
    labResults = labResults != null ? List.copyOf(labResults) : List.of();
    vitalSigns = vitalSigns != null ? List.copyOf(vitalSigns) : List.of();
    sensorObservations = sensorObservations != null ? List.copyOf(sensorObservations) : List.of();
    clinicalHistory = clinicalHistory != null ? List.copyOf(clinicalHistory) : List.of();
    uploadedBy = uploadedBy != null ? uploadedBy : UUID.randomUUID();
    executionContext = executionContext != null ? executionContext : AIExecutionContext.of("default", uploadedBy.toString());
    metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
  }
}
