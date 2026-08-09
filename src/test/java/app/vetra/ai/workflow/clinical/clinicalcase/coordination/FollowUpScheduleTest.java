package app.vetra.ai.workflow.clinical.clinicalcase.coordination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskActor;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FollowUpScheduleTest {

  @Test
  void testCanonicalConstructorAndDefaults() {
    UUID caseId = UUID.randomUUID();
    Instant now = Instant.now();

    FollowUpSchedule schedule = new FollowUpSchedule(
        UUID.randomUUID(),
        caseId,
        UUID.randomUUID(),
        UUID.randomUUID(),
        now.plusSeconds(86400),
        FollowUpScheduleStatus.SCHEDULED,
        "Re-examine respiratory parameters",
        List.of("Lung sounds", "Temperature"),
        List.of("Temp > 40.0C"),
        CareTaskActor.VETERINARIAN,
        now);

    assertNotNull(schedule);
    assertEquals(caseId, schedule.caseId());
    assertEquals(FollowUpScheduleStatus.SCHEDULED, schedule.status());
    assertEquals(CareTaskActor.VETERINARIAN, schedule.responsibleActor());
    assertEquals(2, schedule.expectedObservations().size());
  }
}
