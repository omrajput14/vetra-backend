package app.vetra.ai.workflow.clinical.step;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import app.vetra.ai.event.ClinicalActionPlanGeneratedEvent;
import app.vetra.ai.observability.AIMetricsCollector;
import app.vetra.ai.workflow.clinical.action.ClinicalActionPlanEngine;
import app.vetra.ai.workflow.clinical.model.ClinicalWorkflowContext;
import app.vetra.ai.workflow.clinical.model.ClinicalWorkflowRequest;
import app.vetra.ai.workflow.clinical.model.WorkflowStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class ActionPlanStepTest {

  private ClinicalActionPlanEngine engine;
  private ApplicationEventPublisher eventPublisher;
  private AIMetricsCollector metricsCollector;
  private ActionPlanStep step;

  @BeforeEach
  void setUp() {
    engine = new ClinicalActionPlanEngine();
    eventPublisher = mock(ApplicationEventPublisher.class);
    metricsCollector = mock(AIMetricsCollector.class);
    step = new ActionPlanStep(engine, eventPublisher, metricsCollector);
  }

  @Test
  void testStepProperties() {
    assertEquals("action_plan", step.stepName());
    assertEquals(8, step.order());
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

    assertNotNull(context.getActionPlan());
    assertEquals(WorkflowStatus.SUCCESS, context.getStepStatuses().get("action_plan"));

    verify(eventPublisher).publishEvent(any(ClinicalActionPlanGeneratedEvent.class));
  }
}
