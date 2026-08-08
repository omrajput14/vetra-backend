package app.vetra.ai.workflow.clinical;

import app.vetra.ai.event.ClinicalWorkflowCompletedEvent;
import app.vetra.ai.event.ClinicalWorkflowFailedEvent;
import app.vetra.ai.event.ClinicalWorkflowStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Asynchronous / decoupled event listener that reacts to clinical diagnosis workflow events.
 *
 * <p>Separates database persistence, notifications, and telemetry dispatch from core orchestration logic.
 */
@Component
public class ClinicalWorkflowEventListener {

  private static final Logger log = LoggerFactory.getLogger(ClinicalWorkflowEventListener.class);

  /**
   * Handles workflow initiation event.
   *
   * @param event initiation domain event
   */
  @EventListener
  public void onWorkflowStarted(ClinicalWorkflowStartedEvent event) {
    log.info(
        "Clinical Workflow STARTED: scanId={} animalId={} timestamp={}",
        event.scanId(),
        event.animalId(),
        event.timestamp());
  }

  /**
   * Handles workflow completion event.
   *
   * @param event completion domain event
   */
  @EventListener
  public void onWorkflowCompleted(ClinicalWorkflowCompletedEvent event) {
    log.info(
        "Clinical Workflow COMPLETED: scanId={} animalId={} reportId={} primaryDiagnosis='{}' confidence={} durationMs={}",
        event.scanId(),
        event.animalId(),
        event.reportId(),
        event.primaryDiagnosis(),
        event.confidenceScore(),
        event.durationMs());
  }

  /**
   * Handles triage step completion event.
   *
   * @param event triage completion domain event
   */
  @EventListener
  public void onTriageCompleted(app.vetra.ai.event.ClinicalTriageCompletedEvent event) {
    log.info(
        "Clinical Triage COMPLETED: scanId={} animalId={} urgency={} durationMs={}",
        event.scanId(),
        event.animalId(),
        event.urgency(),
        event.durationMs());
  }

  /**
   * Handles workflow failure event.
   *
   * @param event failure domain event
   */
  @EventListener
  public void onWorkflowFailed(ClinicalWorkflowFailedEvent event) {
    log.error(
        "Clinical Workflow FAILED: scanId={} animalId={} step={} error='{}' durationMs={}",
        event.scanId(),
        event.animalId(),
        event.failedStep(),
        event.errorMessage(),
        event.durationMs());
  }
}
