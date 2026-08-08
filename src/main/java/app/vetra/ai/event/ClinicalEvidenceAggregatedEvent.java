package app.vetra.ai.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when multi-modal clinical evidence has been aggregated for a workflow execution.
 *
 * <p>Contains low-cardinality metadata only (no PII, raw measurements, or patient identifiers).
 *
 * @param scanId scan identifier
 * @param animalId animal identifier
 * @param totalEvidenceItems total count of aggregated evidence items
 * @param conflictCount count of detected measurement conflicts
 * @param warningCount count of data quality warnings
 * @param timestamp event creation timestamp
 */
public record ClinicalEvidenceAggregatedEvent(
    UUID scanId,
    UUID animalId,
    int totalEvidenceItems,
    int conflictCount,
    int warningCount,
    Instant timestamp) {

  /** Canonical constructor with non-null defaults. */
  public ClinicalEvidenceAggregatedEvent {
    scanId = scanId != null ? scanId : UUID.randomUUID();
    animalId = animalId != null ? animalId : UUID.randomUUID();
    timestamp = timestamp != null ? timestamp : Instant.now();
  }
}
