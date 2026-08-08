package app.vetra.ai.workflow.clinical.step;

import app.vetra.ai.event.ClinicalTriageCompletedEvent;
import app.vetra.ai.workflow.clinical.model.ClinicalWorkflowContext;
import app.vetra.ai.workflow.clinical.model.TriageAssessment;
import app.vetra.ai.workflow.clinical.model.TriageRequest;
import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import app.vetra.ai.workflow.clinical.model.WorkflowStatus;
import app.vetra.ai.workflow.clinical.triage.ClinicalTriageEngine;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Step 4: Determines case urgency and recommends escalation actions via ClinicalTriageEngine.
 */
@Component
public class ClinicalTriageStep implements WorkflowStep {

  private static final Logger log = LoggerFactory.getLogger(ClinicalTriageStep.class);
  public static final String STEP_NAME = "triage";

  private final ClinicalTriageEngine triageEngine;
  private final ApplicationEventPublisher eventPublisher;

  /**
   * Constructs ClinicalTriageStep.
   *
   * @param triageEngine 2-layer triage evaluation engine
   * @param eventPublisher domain event publisher
   */
  public ClinicalTriageStep(
      ClinicalTriageEngine triageEngine, ApplicationEventPublisher eventPublisher) {
    this.triageEngine = triageEngine;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public void execute(ClinicalWorkflowContext context) throws Exception {
    long start = System.currentTimeMillis();
    log.info("Executing ClinicalTriageStep for scanId={}", context.getRequest().scanId());

    try {
      TriageRequest triageRequest = buildTriageRequest(context);
      TriageAssessment assessment = triageEngine.assessTriage(triageRequest);

      context.setTriageAssessment(assessment);

      long duration = System.currentTimeMillis() - start;
      context.recordStepTiming(STEP_NAME, duration);
      context.recordStepStatus(STEP_NAME, WorkflowStatus.SUCCESS);

      log.info(
          "ClinicalTriageStep completed in {}ms for scanId={} urgency={}",
          duration,
          context.getRequest().scanId(),
          assessment.urgency());

      if (eventPublisher != null) {
        eventPublisher.publishEvent(
            new ClinicalTriageCompletedEvent(
                context.getRequest().scanId(),
                context.getRequest().animalId(),
                assessment.urgency(),
                Instant.now(),
                duration));
      }

    } catch (Exception ex) {
      long duration = System.currentTimeMillis() - start;
      context.recordStepTiming(STEP_NAME, duration);
      context.recordStepStatus(STEP_NAME, WorkflowStatus.PARTIAL);
      context.addError("ClinicalTriageStep partial failure: " + ex.getMessage());

      log.warn(
          "ClinicalTriageStep partial failure for scanId={}: {}. Applying conservative fallback.",
          context.getRequest().scanId(),
          ex.getMessage());

      TriageAssessment fallback = TriageAssessment.conservativeFallback(ex.getMessage());
      context.setTriageAssessment(fallback);

      if (eventPublisher != null) {
        eventPublisher.publishEvent(
            new ClinicalTriageCompletedEvent(
                context.getRequest().scanId(),
                context.getRequest().animalId(),
                TriageUrgency.URGENT,
                Instant.now(),
                duration));
      }
    }
  }

  private TriageRequest buildTriageRequest(ClinicalWorkflowContext context) {
    String retrievedEvidence =
        context.getRetrievedContext() != null && context.getRetrievedContext().contextText() != null
            ? context.getRetrievedContext().contextText()
            : "";

    return new TriageRequest(
        context.getRequest().species(),
        context.getRequest().breed(),
        context.getRequest().symptoms(),
        List.of(),
        context.getRankedDiseases(),
        retrievedEvidence,
        context.getUnifiedEvidence(),
        context.getRequest().metadata(),
        context.getRequest().executionContext());
  }

  @Override
  public String stepName() {
    return STEP_NAME;
  }

  @Override
  public int order() {
    return 5;
  }
}
