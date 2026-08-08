package app.vetra.ai.workflow.clinical;

import app.vetra.ai.event.ClinicalWorkflowCompletedEvent;
import app.vetra.ai.event.ClinicalWorkflowFailedEvent;
import app.vetra.ai.event.ClinicalWorkflowStartedEvent;
import app.vetra.ai.observability.AIDashboardMetadata;
import app.vetra.ai.observability.AIMetricsCollector;
import app.vetra.ai.observability.AIObservationConvention;
import app.vetra.ai.workflow.clinical.model.ClinicalDiagnosisReport;
import app.vetra.ai.workflow.clinical.model.ClinicalWorkflowContext;
import app.vetra.ai.workflow.clinical.model.ClinicalWorkflowRequest;
import app.vetra.ai.workflow.clinical.model.ClinicalWorkflowResult;
import app.vetra.ai.workflow.clinical.model.WorkflowStatus;
import app.vetra.ai.workflow.clinical.step.DiagnosisStep;
import app.vetra.ai.workflow.clinical.step.KnowledgeStep;
import app.vetra.ai.workflow.clinical.step.RankingStep;
import app.vetra.ai.workflow.clinical.step.ReportStep;
import app.vetra.ai.workflow.clinical.step.TreatmentStep;
import app.vetra.ai.workflow.clinical.step.WorkflowStep;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Enterprise workflow orchestration engine coordinating multi-agent clinical diagnosis stages.
 *
 * <p>Coordinates polymorphic {@link WorkflowStep} components (DiagnosisStep, KnowledgeStep,
 * RankingStep, TreatmentStep, ReportStep) over a shared {@link ClinicalWorkflowContext},
 * emits OpenTelemetry span events, publishes domain events, and records operational metrics.
 */
@Service
public class ClinicalWorkflowEngine {

  private static final Logger log = LoggerFactory.getLogger(ClinicalWorkflowEngine.class);

  private final List<WorkflowStep> steps;
  private final AIMetricsCollector metricsCollector;
  private final AIObservationConvention observationConvention;
  private final ApplicationEventPublisher eventPublisher;

  /**
   * Constructs ClinicalWorkflowEngine.
   *
   * @param steps list of registered workflow step beans (auto-sorted by order)
   * @param metricsCollector operational metrics collector
   * @param observationConvention OpenTelemetry span event helper
   * @param eventPublisher Spring domain event publisher
   */
  public ClinicalWorkflowEngine(
      List<WorkflowStep> steps,
      AIMetricsCollector metricsCollector,
      @Autowired(required = false) AIObservationConvention observationConvention,
      ApplicationEventPublisher eventPublisher) {
    this.steps = steps.stream().sorted(Comparator.comparingInt(WorkflowStep::order)).toList();
    this.metricsCollector = metricsCollector;
    this.observationConvention = observationConvention;
    this.eventPublisher = eventPublisher;
  }

  /**
   * Executes the full multi-agent clinical diagnosis workflow.
   *
   * @param request workflow initiation parameters
   * @return {@link ClinicalWorkflowResult} containing the synthesized report and context
   */
  public ClinicalWorkflowResult executeWorkflow(ClinicalWorkflowRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("ClinicalWorkflowRequest cannot be null");
    }

    long startNanos = System.nanoTime();
    log.info("Starting Clinical Diagnosis Workflow for scanId={} animalId={}", request.scanId(), request.animalId());

    ClinicalWorkflowContext context = new ClinicalWorkflowContext(request);
    eventPublisher.publishEvent(new ClinicalWorkflowStartedEvent(request.scanId(), request.animalId(), Instant.now()));

    try {
      for (WorkflowStep step : steps) {
        log.debug("Executing workflow step: {} (order={})", step.stepName(), step.order());
        step.execute(context);
        recordSpanEventForStep(step.stepName());
        recordMetricsForStep(step.stepName());
      }

      long totalDurationMs = (System.nanoTime() - startNanos) / 1_000_000L;
      context.setTotalDurationMs(totalDurationMs);

      WorkflowStatus finalStatus =
          context.getErrors().isEmpty() ? WorkflowStatus.SUCCESS : WorkflowStatus.PARTIAL;
      context.setStatus(finalStatus);

      ClinicalDiagnosisReport report = context.getReport();
      if (report == null) {
        throw new IllegalStateException("Workflow completed without generating a ClinicalDiagnosisReport");
      }

      if (metricsCollector != null) {
        metricsCollector.recordClinicalWorkflow(finalStatus.name(), System.nanoTime() - startNanos);
      }

      log.info(
          "Clinical Diagnosis Workflow COMPLETED in {}ms with status={} for scanId={}",
          totalDurationMs,
          finalStatus,
          request.scanId());

      eventPublisher.publishEvent(
          new ClinicalWorkflowCompletedEvent(
              request.scanId(),
              request.animalId(),
              report.reportId(),
              report.primaryDiagnosis(),
              report.confidenceScore(),
              totalDurationMs,
              report));

      return new ClinicalWorkflowResult(report, context, finalStatus, totalDurationMs);

    } catch (Exception ex) {
      long durationMs = (System.nanoTime() - startNanos) / 1_000_000L;
      log.error("Clinical Diagnosis Workflow FAILED after {}ms for scanId={}: {}", durationMs, request.scanId(), ex.getMessage());

      context.setStatus(WorkflowStatus.FAILED);
      context.addError("Workflow failed: " + ex.getMessage());

      if (metricsCollector != null) {
        metricsCollector.recordClinicalWorkflow(WorkflowStatus.FAILED.name(), System.nanoTime() - startNanos);
      }

      eventPublisher.publishEvent(
          new ClinicalWorkflowFailedEvent(
              request.scanId(),
              request.animalId(),
              ex.getMessage(),
              "WORKFLOW_EXECUTION",
              durationMs));

      if (ex instanceof RuntimeException rte) {
        throw rte;
      }
      throw new RuntimeException("Clinical workflow execution failed: " + ex.getMessage(), ex);
    }
  }

  private void recordSpanEventForStep(String stepName) {
    if (observationConvention == null) {
      return;
    }
    switch (stepName) {
      case DiagnosisStep.STEP_NAME -> observationConvention.recordSpanEvent(AIDashboardMetadata.SPAN_EVENT_DIAGNOSIS_COMPLETED);
      case KnowledgeStep.STEP_NAME -> observationConvention.recordSpanEvent(AIDashboardMetadata.SPAN_EVENT_RETRIEVAL_COMPLETED);
      case RankingStep.STEP_NAME -> observationConvention.recordSpanEvent(AIDashboardMetadata.SPAN_EVENT_RANKING_COMPLETED);
      case TreatmentStep.STEP_NAME -> observationConvention.recordSpanEvent(AIDashboardMetadata.SPAN_EVENT_TREATMENT_COMPLETED);
      case ReportStep.STEP_NAME -> observationConvention.recordSpanEvent(AIDashboardMetadata.SPAN_EVENT_REPORT_GENERATED);
      default -> observationConvention.recordSpanEvent(stepName + " completed");
    }
  }

  private void recordMetricsForStep(String stepName) {
    if (metricsCollector == null) {
      return;
    }
    switch (stepName) {
      case RankingStep.STEP_NAME -> metricsCollector.recordDiseaseRanking(AIDashboardMetadata.STATUS_SUCCESS);
      case TreatmentStep.STEP_NAME -> metricsCollector.recordTreatmentGeneration(AIDashboardMetadata.STATUS_SUCCESS);
      default -> {
        // Handled in main workflow or agent
      }
    }
  }

  /**
   * Returns unmodifiable view of registered steps.
   *
   * @return ordered list of {@link WorkflowStep}
   */
  public List<WorkflowStep> getSteps() {
    return steps;
  }
}
