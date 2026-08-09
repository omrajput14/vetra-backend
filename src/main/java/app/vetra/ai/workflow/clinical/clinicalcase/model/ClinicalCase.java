package app.vetra.ai.workflow.clinical.clinicalcase.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable longitudinal identity of a veterinary clinical case.
 *
 * @param caseId unique case identifier
 * @param animalId linked target animal identifier
 * @param species target animal species
 * @param breed target animal breed
 * @param primaryCondition primary working diagnosis
 * @param openedAt case creation timestamp
 * @param lastUpdatedAt latest modification timestamp
 * @param closedAt case resolution timestamp (optional)
 * @param status current lifecycle status
 */
public record ClinicalCase(
    UUID caseId,
    UUID animalId,
    String species,
    String breed,
    String primaryCondition,
    Instant openedAt,
    Instant lastUpdatedAt,
    Instant closedAt,
    ClinicalCaseStatus status) {

  /** Canonical constructor with non-null defaults. */
  public ClinicalCase {
    caseId = caseId != null ? caseId : UUID.randomUUID();
    animalId = animalId != null ? animalId : UUID.randomUUID();
    species = species != null ? species.trim() : "UNKNOWN";
    breed = breed != null ? breed.trim() : "UNKNOWN";
    primaryCondition = primaryCondition != null ? primaryCondition.trim() : "Unspecified Clinical Observation";
    openedAt = openedAt != null ? openedAt : Instant.now();
    lastUpdatedAt = lastUpdatedAt != null ? lastUpdatedAt : Instant.now();
    status = status != null ? status : ClinicalCaseStatus.OPEN;
  }
}
