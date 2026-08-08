package app.vetra.ai.workflow.clinical.step;

import app.vetra.ai.event.ClinicalDecisionSupportGeneratedEvent;
import app.vetra.ai.observability.AIDashboardMetadata;
import app.vetra.ai.observability.AIMetricsCollector;
import app.vetra.ai.observability.AIObservationConvention;
import app.vetra.ai.workflow.clinical.explainability.ClinicalDecisionSupportEngine;
import app.vetra.ai.workflow.clinical.model.ClinicalWorkflowContext;
import app.vetra.ai.workflow.clinical.model.WorkflowStatus;
import app.vetra.ai.workflow.clinical.model.explainability.ClinicalDecisionSupport;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Workflow step (Order 7) responsible for generating deterministic clinical decision support,
 * evidence traceability, uncertainty quantification, and veterinarian review flags.
 */
@Component
public class DecisionSupportStep implements WorkflowStep {

  public static final String STEP_NAME = "decision_support";
  private static final Logger log = LoggerFactory.getLogger(DecisionSupportStep.class);

  private final ClinicalDecisionSupportEngine engine;
  private final ApplicationEventPublisher eventPublisher;
  private final AIMetricsCollector metricsCollector;
  private final AIObservationConvention observationConvention;

  public DecisionSupportStep(
      ClinicalDecisionSupportEngine engine,
      ApplicationEventPublisher eventPublisher,
      AIMetricsCollector metricsCollector) {
    this(engine, eventPublisher, metricsCollector, null);
  }

  @Autowired
  public DecisionSupportStep(
      ClinicalDecisionSupportEngine engine,
      @Autowired(required = false) ApplicationEventPublisher eventPublisher,
      @Autowired(required = false) AIMetricsCollector metricsCollector,
      @Autowired(required = false) AIObservationConvention observationConvention) {
    this.engine = engine;
    this.eventPublisher = eventPublisher;
    this.metricsCollector = metricsCollector;
    this.observationConvention = observationConvention;
  }

  @Override
  public void execute(ClinicalWorkflowContext context) throws Exception {
    long startNanos = System.nanoTime();
    log.debug("Executing DecisionSupportStep (Order 7)");

    if (observationConvention != null) {
      observationConvention.recordSpanEvent(AIDashboardMetadata.SPAN_EVENT_DECISION_SUPPORT_STARTED);
    }

    try {
      ClinicalDecisionSupport cds = engine.evaluate(context);
      context.setDecisionSupport(cds);
      context.recordStepStatus(stepName(), WorkflowStatus.SUCCESS);

      long durationMs = (System.nanoTime() - startNanos) / 1_000_000L;
      context.recordStepTiming(stepName(), durationMs);

      boolean requiresReview = cds.veterinarianReviewFlag().requiresReview();
      String uncertaintyLevel = cds.uncertaintyAssessment().overallLevel().name();

      if (metricsCollector != null) {
        metricsCollector.recordClinicalExplanation(requiresReview, uncertaintyLevel);
        metricsCollector.recordClinicalUncertainty(uncertaintyLevel);
        if (requiresReview && !cds.veterinarianReviewFlag().reasonCategories().isEmpty()) {
          metricsCollector.recordClinicalReviewRequired(
              cds.veterinarianReviewFlag().reasonCategories().get(0).name());
        }
      }

      if (observationConvention != null) {
        observationConvention.recordSpanEvent(AIDashboardMetadata.SPAN_EVENT_DIAGNOSIS_EXPLANATION_GENERATED);
        observationConvention.recordSpanEvent(AIDashboardMetadata.SPAN_EVENT_TRIAGE_EXPLANATION_GENERATED);
        if (requiresReview) {
          observationConvention.recordSpanEvent(AIDashboardMetadata.SPAN_EVENT_VETERINARIAN_REVIEW_REQUIRED);
        }
      }

      if (eventPublisher != null) {
        eventPublisher.publishEvent(
            new ClinicalDecisionSupportGeneratedEvent(
                context.getRequest().scanId(),
                context.getRequest().animalId(),
                requiresReview,
                uncertaintyLevel,
                Instant.now()));
      }

      log.info(
          "DecisionSupportStep completed in {} ms: requiresReview={}, uncertainty={}",
          durationMs,
          requiresReview,
          uncertaintyLevel);

    } catch (Exception e) {
      log.error("DecisionSupportStep failed", e);
      context.addError("Decision support error: " + e.getMessage());
      context.recordStepStatus(stepName(), WorkflowStatus.FAILED);
      throw e;
    }
  }

  @Override
  public String stepName() {
    return STEP_NAME;
  }

  @Override
  public int order() {
    return 7;
  }
}
