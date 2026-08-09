package app.vetra.ai.workflow.clinical.clinicalcase.task.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskActor;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskPriority;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskType;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.ClinicalCareTask;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CareTaskEscalationEngineTest {

  private CareTaskEscalationEngine engine;

  @BeforeEach
  void setUp() {
    engine = new CareTaskEscalationEngine();
  }

  @Test
  void testEscalateOverdueMandatoryVetTask() {
    UUID caseId = UUID.randomUUID();
    Instant now = Instant.now();

    ClinicalCareTask task = new ClinicalCareTask(
        UUID.randomUUID(),
        caseId,
        UUID.randomUUID(),
        CareTaskType.VETERINARIAN_REVIEW,
        CareTaskPriority.HIGH,
        CareTaskActor.VETERINARIAN,
        CareTaskStatus.OVERDUE,
        "Mandatory Vet Review",
        "Review critical lab results",
        now.minusSeconds(7200),
        now.minusSeconds(3600),
        null,
        true,
        true,
        false,
        List.of(),
        List.of(),
        null,
        null,
        "TEST");

    List<ClinicalCareTask> escalated = engine.evaluateEscalations(List.of(task), null, null, null, null, now);

    assertNotNull(escalated);
    assertFalse(escalated.isEmpty());
    assertEquals(CareTaskPriority.EMERGENCY, escalated.get(0).priority());
    assertEquals(CareTaskStatus.ESCALATED, escalated.get(0).status());
  }

  @Test
  void testCompletedTaskIsNotEscalated() {
    UUID caseId = UUID.randomUUID();
    Instant now = Instant.now();

    ClinicalCareTask task = new ClinicalCareTask(
        UUID.randomUUID(),
        caseId,
        UUID.randomUUID(),
        CareTaskType.VETERINARIAN_REVIEW,
        CareTaskPriority.HIGH,
        CareTaskActor.VETERINARIAN,
        CareTaskStatus.COMPLETED,
        "Completed Review",
        "Finished",
        now.minusSeconds(7200),
        now.minusSeconds(3600),
        now.minusSeconds(1800),
        true,
        true,
        false,
        List.of(),
        List.of(),
        null,
        null,
        "TEST");

    List<ClinicalCareTask> escalated = engine.evaluateEscalations(List.of(task), null, null, null, null, now);

    assertNotNull(escalated);
    assertTrue(escalated.isEmpty());
  }
}
