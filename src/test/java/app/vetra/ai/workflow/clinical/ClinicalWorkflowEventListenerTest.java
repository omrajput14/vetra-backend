package app.vetra.ai.workflow.clinical;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import app.vetra.ai.event.ClinicalWorkflowCompletedEvent;
import app.vetra.ai.event.ClinicalWorkflowFailedEvent;
import app.vetra.ai.event.ClinicalWorkflowStartedEvent;
import app.vetra.ai.workflow.clinical.model.ClinicalDiagnosisReport;
import app.vetra.ai.workflow.clinical.model.WorkflowStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClinicalWorkflowEventListenerTest {

  private ClinicalWorkflowEventListener listener;

  @BeforeEach
  void setUp() {
    listener = new ClinicalWorkflowEventListener();
  }

  @Test
  void testEventListener_handlesEventsWithoutExceptions() {
    UUID scanId = UUID.randomUUID();
    UUID animalId = UUID.randomUUID();
    UUID reportId = UUID.randomUUID();

    assertDoesNotThrow(
        () -> listener.onWorkflowStarted(new ClinicalWorkflowStartedEvent(scanId, animalId, Instant.now())));

    ClinicalDiagnosisReport report =
        new ClinicalDiagnosisReport(
            reportId,
            scanId,
            animalId,
            "Cattle",
            List.of(),
            List.of(),
            "Foot and Mouth Disease",
            BigDecimal.valueOf(0.95),
            "Vesicles",
            "Literature",
            "Treatment",
            List.of(),
            List.of(),
            List.of(),
            Instant.now(),
            Map.of(),
            120L,
            WorkflowStatus.SUCCESS);

    assertDoesNotThrow(
        () ->
            listener.onWorkflowCompleted(
                new ClinicalWorkflowCompletedEvent(
                    scanId,
                    animalId,
                    reportId,
                    "Foot and Mouth Disease",
                    BigDecimal.valueOf(0.95),
                    120L,
                    report)));

    assertDoesNotThrow(
        () ->
            listener.onWorkflowFailed(
                new ClinicalWorkflowFailedEvent(
                    scanId, animalId, "Timeout", "diagnosis", 50L)));
  }
}
