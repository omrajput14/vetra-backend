package app.vetra.ai.workflow.clinical.model.evidence;

import java.time.Instant;

/**
 * Immutable record representing a prior clinical event, past diagnosis, vaccination, or medical history entry.
 *
 * @param conditionOrEvent condition name, procedure, or vaccination event
 * @param diagnosedAt timestamp or date of prior event
 * @param treatmentGiven past treatment or intervention provided
 * @param outcomeStatus resolution or outcome status (RESOLVED, ONGOING, CHRONIC)
 * @param notes additional historical notes
 */
public record ClinicalHistory(
    String conditionOrEvent,
    Instant diagnosedAt,
    String treatmentGiven,
    String outcomeStatus,
    String notes) {

  /** Canonical constructor with non-null defaults. */
  public ClinicalHistory {
    conditionOrEvent = conditionOrEvent != null ? conditionOrEvent.trim() : "Unspecified Prior Event";
    diagnosedAt = diagnosedAt != null ? diagnosedAt : Instant.now();
    treatmentGiven = treatmentGiven != null ? treatmentGiven.trim() : "None";
    outcomeStatus = outcomeStatus != null ? outcomeStatus.trim() : "RESOLVED";
    notes = notes != null ? notes.trim() : "";
  }
}
