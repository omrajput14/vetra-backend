package app.vetra.ai.workflow.clinical.clinicalcase.operations;

import app.vetra.ai.workflow.clinical.clinicalcase.model.ClinicalCaseStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.ClinicalCareTask;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Immutable operational read-model projection tailored specifically for farmers and caregivers.
 *
 * <p>Enforces strict data minimization: excludes diagnostic uncertainty internals, evidence conflicts,
 * RAG citations, AI/provider metadata, telemetry, and internal task provenance.
 */
public record FarmerOperationalCaseView(
    UUID caseId,
    UUID animalId,
    String species,
    String breed,
    String primaryCondition,
    ClinicalCaseStatus status,
    boolean veterinarianReviewRequired,
    List<ClinicalCareTask> immediateCareTasks,
    List<ClinicalCareTask> monitoringTasks,
    String followUpDueStatus,
    boolean escalationRequired,
    String nextDueAction,
    Instant lastUpdatedAt) {

  /** Canonical constructor with non-null defaults. */
  public FarmerOperationalCaseView {
    caseId = caseId != null ? caseId : UUID.randomUUID();
    animalId = animalId != null ? animalId : UUID.randomUUID();
    species = species != null ? species.trim() : "UNKNOWN";
    breed = breed != null ? breed.trim() : "UNKNOWN";
    primaryCondition = primaryCondition != null ? primaryCondition.trim() : "UNKNOWN";
    status = status != null ? status : ClinicalCaseStatus.OPEN;
    immediateCareTasks = immediateCareTasks != null ? List.copyOf(immediateCareTasks) : List.of();
    monitoringTasks = monitoringTasks != null ? List.copyOf(monitoringTasks) : List.of();
    followUpDueStatus = followUpDueStatus != null ? followUpDueStatus.trim() : "NO_IMMEDIATE_DUE";
    nextDueAction = nextDueAction != null ? nextDueAction.trim() : "";
    lastUpdatedAt = lastUpdatedAt != null ? lastUpdatedAt : Instant.now();
  }
}
