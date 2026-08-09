package app.vetra.ai.workflow.clinical.clinicalcase.timeline;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable historical event entry in a case's chronological timeline.
 *
 * @param eventId unique event identifier
 * @param caseId linked case identifier
 * @param timestamp event occurrence timestamp
 * @param type event classification
 * @param summary human-readable summary
 * @param sourceEncounterId linked encounter ID (optional)
 * @param metadata non-sensitive structural metadata
 */
public record ClinicalTimelineEvent(
    UUID eventId,
    UUID caseId,
    Instant timestamp,
    ClinicalTimelineEventType type,
    String summary,
    UUID sourceEncounterId,
    Map<String, Object> metadata) {

  /** Canonical constructor with non-null defaults. */
  public ClinicalTimelineEvent {
    eventId = eventId != null ? eventId : UUID.randomUUID();
    caseId = caseId != null ? caseId : UUID.randomUUID();
    timestamp = timestamp != null ? timestamp : Instant.now();
    type = type != null ? type : ClinicalTimelineEventType.ENCOUNTER_RECORDED;
    summary = summary != null ? summary.trim() : "";
    metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
  }
}
