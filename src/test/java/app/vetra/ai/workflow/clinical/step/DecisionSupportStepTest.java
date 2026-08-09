package app.vetra.ai.workflow.clinical.step;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import app.vetra.ai.event.ClinicalDecisionSupportGeneratedEvent;
import app.vetra.ai.observability.AIMetricsCollector;
import app.vetra.ai.workflow.clinical.explainability.ClinicalDecisionSupportEngine;
import app.vetra.ai.workflow.clinical.model.ClinicalWorkflowContext;
import app.vetra.ai.workflow.clinical.model.ClinicalWorkflowRequest;
import app.vetra.ai.workflow.clinical.model.WorkflowStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class DecisionSupportStepTest {

  private ClinicalDecisionSupportEngine engine;
  private ApplicationEventPublisher eventPublisher;
  private AIMetricsCollector metricsCollector;
  private DecisionSupportStep step;

  @BeforeEach
  void setUp() {
    engine = new ClinicalDecisionSupportEngine();
    eventPublisher = mock(ApplicationEventPublisher.class);
    metricsCollector = mock(AIMetricsCollector.class);
    step = new DecisionSupportStep(engine, eventPublisher, metricsCollector);
  }

  @Test
  void testStepProperties() {
    assertEquals("decision_support", step.stepName());
    assertEquals(7, step.order());
  }

  @Test
  void testExecute_populatesContextAndPublishesEvent() throws Exception {
    ClinicalWorkflowRequest request =
        new ClinicalWorkflowRequest(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "BOVINE",
            "Holstein",
            "",
            List.of("Lethargy"),
            UUID.randomUUID(),
            null,
            null);

    ClinicalWorkflowContext context = new ClinicalWorkflowContext(request);
    step.execute(context);

    assertNotNull(context.getDecisionSupport());
    assertEquals(WorkflowStatus.SUCCESS, context.getStepStatuses().get("decision_support"));

    verify(eventPublisher).publishEvent(any(ClinicalDecisionSupportGeneratedEvent.class));
  }
}
