package app.vetra.ai.workflow.clinical.clinicalcase.timeline;

import java.util.List;
import java.util.UUID;

/**
 * Chronological timeline container for a clinical case.
 *
 * @param caseId linked case identifier
 * @param events chronologically sorted timeline events
 */
public record ClinicalCaseTimeline(UUID caseId, List<ClinicalTimelineEvent> events) {

  /** Canonical constructor with non-null defaults. */
  public ClinicalCaseTimeline {
    caseId = caseId != null ? caseId : UUID.randomUUID();
    events = events != null ? List.copyOf(events) : List.of();
  }
}
