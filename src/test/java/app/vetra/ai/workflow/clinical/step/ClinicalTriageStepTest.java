package app.vetra.ai.workflow.clinical.step;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.vetra.ai.event.ClinicalTriageCompletedEvent;
import app.vetra.ai.workflow.clinical.model.ClinicalWorkflowContext;
import app.vetra.ai.workflow.clinical.model.ClinicalWorkflowRequest;
import app.vetra.ai.workflow.clinical.model.TriageAssessment;
import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import app.vetra.ai.workflow.clinical.model.WorkflowStatus;
import app.vetra.ai.workflow.clinical.triage.ClinicalTriageEngine;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class ClinicalTriageStepTest {

  private ClinicalTriageEngine triageEngine;
  private ApplicationEventPublisher eventPublisher;
  private ClinicalTriageStep triageStep;

  @BeforeEach
  void setUp() {
    triageEngine = mock(ClinicalTriageEngine.class);
    eventPublisher = mock(ApplicationEventPublisher.class);
    triageStep = new ClinicalTriageStep(triageEngine, eventPublisher);
  }

  @Test
  void testStepProperties() {
    assertEquals("triage", triageStep.stepName());
    assertEquals(5, triageStep.order());
  }

  @Test
  void testExecute_attachesAssessmentAndPublishesEvent() throws Exception {
    ClinicalWorkflowRequest request =
        new ClinicalWorkflowRequest(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "BOVINE",
            "Holstein",
            "https://cdn.vetra.app/img.jpg",
            List.of("Fever"),
            UUID.randomUUID(),
            null,
            null);

    ClinicalWorkflowContext context = new ClinicalWorkflowContext(request);

    TriageAssessment assessment =
        new TriageAssessment(
            TriageUrgency.PRIORITY,
            BigDecimal.valueOf(0.80),
            "Short term assessment required",
            List.of("Fever"),
            List.of("Vet consultation"),
            false,
            Instant.now());

    when(triageEngine.assessTriage(any())).thenReturn(assessment);

    triageStep.execute(context);

    assertNotNull(context.getTriageAssessment());
    assertEquals(TriageUrgency.PRIORITY, context.getTriageAssessment().urgency());
    assertEquals(WorkflowStatus.SUCCESS, context.getStepStatuses().get("triage"));

    verify(eventPublisher).publishEvent(any(ClinicalTriageCompletedEvent.class));
  }
}
