package app.vetra.ai.workflow.clinical.step;

import app.vetra.ai.event.ClinicalActionPlanGeneratedEvent;
import app.vetra.ai.observability.AIMetricsCollector;
import app.vetra.ai.workflow.clinical.action.ClinicalActionPlanEngine;
import app.vetra.ai.workflow.clinical.model.ClinicalWorkflowContext;
import app.vetra.ai.workflow.clinical.model.WorkflowStatus;
import app.vetra.ai.workflow.clinical.model.action.ClinicalActionPlan;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Step 8: Deterministic Action Plan Synthesis step converting intermediate workflow conclusions
 * into an auditable, prioritized ClinicalActionPlan.
 */
@Component
public class ActionPlanStep implements WorkflowStep {

  private static final Logger log = LoggerFactory.getLogger(ActionPlanStep.class);
  public static final String STEP_NAME = "action_plan";

  private final ClinicalActionPlanEngine actionPlanEngine;
  private final ApplicationEventPublisher eventPublisher;
  private final AIMetricsCollector metricsCollector;

  /**
   * Constructs ActionPlanStep.
   *
   * @param actionPlanEngine pure deterministic action plan engine
   * @param eventPublisher domain event publisher (optional)
   * @param metricsCollector metrics collector (optional)
   */
  @Autowired
  public ActionPlanStep(
      ClinicalActionPlanEngine actionPlanEngine,
      @Autowired(required = false) ApplicationEventPublisher eventPublisher,
      @Autowired(required = false) AIMetricsCollector metricsCollector) {
    this.actionPlanEngine = actionPlanEngine;
    this.eventPublisher = eventPublisher;
    this.metricsCollector = metricsCollector;
  }

  @Override
  public void execute(ClinicalWorkflowContext context) throws Exception {
    long start = System.currentTimeMillis();
    log.info("Executing ActionPlanStep for scanId={}", context.getRequest().scanId());

    try {
      ClinicalActionPlan plan = actionPlanEngine.synthesizePlan(context);
      context.setActionPlan(plan);

      long duration = System.currentTimeMillis() - start;
      context.recordStepTiming(STEP_NAME, duration);
      context.recordStepStatus(STEP_NAME, WorkflowStatus.SUCCESS);

      if (eventPublisher != null) {
        int actionCount = plan.immediateActions().size()
            + plan.prioritizedActions().size()
            + plan.monitoringActions().size()
            + plan.followUpActions().size();

        eventPublisher.publishEvent(
            new ClinicalActionPlanGeneratedEvent(
                context.getRequest().scanId(),
                context.getRequest().animalId(),
                plan.planId(),
                plan.urgency(),
                plan.veterinarianReviewRequired(),
                actionCount,
                Instant.now()));
      }

      if (metricsCollector != null) {
        metricsCollector.recordClinicalActionPlan(
            plan.urgency().name(),
            plan.veterinarianReviewRequired());
      }

      log.info(
          "ActionPlanStep completed in {}ms for scanId={}, totalActions={}",
          duration,
          context.getRequest().scanId(),
          plan.immediateActions().size() + plan.prioritizedActions().size());

    } catch (Exception ex) {
      long duration = System.currentTimeMillis() - start;
      context.recordStepTiming(STEP_NAME, duration);
      context.recordStepStatus(STEP_NAME, WorkflowStatus.FAILED);
      context.addError("ActionPlanStep failed: " + ex.getMessage());
      log.error("ActionPlanStep failed for scanId={}: {}", context.getRequest().scanId(), ex.getMessage());
      throw ex;
    }
  }

  @Override
  public String stepName() {
    return STEP_NAME;
  }

  @Override
  public int order() {
    return 8;
  }
}
