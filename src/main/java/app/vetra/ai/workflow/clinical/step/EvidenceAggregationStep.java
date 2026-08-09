package app.vetra.ai.workflow.clinical.step;

import app.vetra.ai.event.ClinicalEvidenceAggregatedEvent;
import app.vetra.ai.observability.AIDashboardMetadata;
import app.vetra.ai.observability.AIMetricsCollector;
import app.vetra.ai.observability.AIObservationConvention;
import app.vetra.ai.workflow.clinical.evidence.ClinicalEvidenceAggregator;
import app.vetra.ai.workflow.clinical.model.ClinicalWorkflowContext;
import app.vetra.ai.workflow.clinical.model.WorkflowStatus;
import app.vetra.ai.workflow.clinical.model.evidence.UnifiedClinicalEvidence;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Workflow step (Order 2) responsible for aggregating multi-modal clinical evidence streams
 * (Symptoms, Visual Pathology, Labs, Vitals, Sensors, History) into a UnifiedClinicalEvidence collection.
 */
@Component
public class EvidenceAggregationStep implements WorkflowStep {

  private static final Logger log = LoggerFactory.getLogger(EvidenceAggregationStep.class);

  private final ClinicalEvidenceAggregator aggregator;
  private final ApplicationEventPublisher eventPublisher;
  private final AIMetricsCollector metricsCollector;
  private final AIObservationConvention observationConvention;

  public EvidenceAggregationStep(
      ClinicalEvidenceAggregator aggregator,
      ApplicationEventPublisher eventPublisher,
      AIMetricsCollector metricsCollector) {
    this(aggregator, eventPublisher, metricsCollector, null);
  }

  @Autowired
  public EvidenceAggregationStep(
      ClinicalEvidenceAggregator aggregator,
      @Autowired(required = false) ApplicationEventPublisher eventPublisher,
      @Autowired(required = false) AIMetricsCollector metricsCollector,
      @Autowired(required = false) AIObservationConvention observationConvention) {
    this.aggregator = aggregator;
    this.eventPublisher = eventPublisher;
    this.metricsCollector = metricsCollector;
    this.observationConvention = observationConvention;
  }

  @Override
  public void execute(ClinicalWorkflowContext context) throws Exception {
    long startNanos = System.nanoTime();
    log.debug("Executing EvidenceAggregationStep (Order 2)");

    if (observationConvention != null) {
      observationConvention.recordSpanEvent(AIDashboardMetadata.SPAN_EVENT_EVIDENCE_AGGREGATION_STARTED);
    }

    try {
      UnifiedClinicalEvidence unified =
          aggregator.aggregateEvidence(
              context.getRequest().symptoms(),
              context.getDiagnosisResponse(),
              context.getRequest().labResults(),
              context.getRequest().vitalSigns(),
              context.getRequest().sensorObservations(),
              context.getRequest().clinicalHistory());

      context.setUnifiedEvidence(unified);
      context.recordStepStatus(stepName(), WorkflowStatus.SUCCESS);

      long durationMs = (System.nanoTime() - startNanos) / 1_000_000L;
      context.recordStepTiming(stepName(), durationMs);

      if (metricsCollector != null) {
        metricsCollector.recordEvidenceProcessing(unified.items().size(), unified.conflicts().size(), durationMs);
      }
      if (observationConvention != null) {
        observationConvention.recordSpanEvent(AIDashboardMetadata.SPAN_EVENT_EVIDENCE_AGGREGATION_COMPLETED);
        if (!unified.conflicts().isEmpty()) {
          observationConvention.recordSpanEvent(AIDashboardMetadata.SPAN_EVENT_EVIDENCE_CONFLICT_DETECTED);
        }
      }

      if (eventPublisher != null) {
        eventPublisher.publishEvent(
            new ClinicalEvidenceAggregatedEvent(
                context.getRequest().scanId(),
                context.getRequest().animalId(),
                unified.items().size(),
                unified.conflicts().size(),
                unified.warnings().size(),
                Instant.now()));
      }

      log.info(
          "EvidenceAggregationStep completed: {} evidence items, {} conflicts in {} ms",
          unified.items().size(),
          unified.conflicts().size(),
          durationMs);

    } catch (Exception e) {
      log.error("EvidenceAggregationStep failed", e);
      context.addError("Evidence aggregation error: " + e.getMessage());
      context.recordStepStatus(stepName(), WorkflowStatus.FAILED);
      throw e;
    }
  }

  @Override
  public String stepName() {
    return "evidence_aggregation";
  }

  @Override
  public int order() {
    return 2;
  }
}
