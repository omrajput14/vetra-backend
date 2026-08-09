package app.vetra.ai.workflow.clinical.step;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import app.vetra.ai.observability.AIMetricsCollector;
import app.vetra.ai.workflow.clinical.evidence.ClinicalEvidenceAggregator;
import app.vetra.ai.workflow.clinical.model.ClinicalWorkflowContext;
import app.vetra.ai.workflow.clinical.model.ClinicalWorkflowRequest;
import app.vetra.ai.workflow.clinical.model.WorkflowStatus;
import app.vetra.ai.workflow.clinical.model.evidence.LaboratoryResult;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class EvidenceAggregationStepTest {

  private ClinicalEvidenceAggregator aggregator;
  private ApplicationEventPublisher eventPublisher;
  private AIMetricsCollector metricsCollector;
  private EvidenceAggregationStep step;

  @BeforeEach
  void setUp() {
    aggregator = new ClinicalEvidenceAggregator();
    eventPublisher = mock(ApplicationEventPublisher.class);
    metricsCollector = mock(AIMetricsCollector.class);
    step = new EvidenceAggregationStep(aggregator, eventPublisher, metricsCollector);
  }

  @Test
  void testStepProperties() {
    assertEquals("evidence_aggregation", step.stepName());
    assertEquals(2, step.order());
  }

  @Test
  void testStepExecution() throws Exception {
    LaboratoryResult lab = new LaboratoryResult("SCC", "500k", "cells/mL", "<200k", null, null, null);

    ClinicalWorkflowRequest request =
        new ClinicalWorkflowRequest(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "BOVINE",
            "Holstein",
            "",
            List.of("Fever"),
            List.of(lab),
            List.of(),
            List.of(),
            List.of(),
            UUID.randomUUID(),
            null,
            null);

    ClinicalWorkflowContext context = new ClinicalWorkflowContext(request);
    step.execute(context);

    assertNotNull(context.getUnifiedEvidence());
    assertEquals(WorkflowStatus.SUCCESS, context.getStepStatuses().get("evidence_aggregation"));
    verify(eventPublisher).publishEvent(any(app.vetra.ai.event.ClinicalEvidenceAggregatedEvent.class));
  }
}
