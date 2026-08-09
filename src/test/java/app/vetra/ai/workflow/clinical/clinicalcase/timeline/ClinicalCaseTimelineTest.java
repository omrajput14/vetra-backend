package app.vetra.ai.workflow.clinical.clinicalcase.timeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClinicalCaseTimelineTest {

  @Test
  void testCanonicalConstructorAndImmutability() {
    UUID caseId = UUID.randomUUID();
    ClinicalTimelineEvent event =
        new ClinicalTimelineEvent(
            UUID.randomUUID(),
            caseId,
            Instant.now(),
            ClinicalTimelineEventType.CASE_OPENED,
            "Case opened",
            null,
            Map.of("status", "OPEN"));

    ClinicalCaseTimeline timeline = new ClinicalCaseTimeline(caseId, List.of(event));

    assertNotNull(timeline);
    assertEquals(caseId, timeline.caseId());
    assertEquals(1, timeline.events().size());
    assertEquals(ClinicalTimelineEventType.CASE_OPENED, timeline.events().get(0).type());
  }
}
