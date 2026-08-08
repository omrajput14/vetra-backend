package app.vetra.ai.workflow.clinical.step;

import app.vetra.ai.workflow.clinical.model.ClinicalWorkflowContext;

/**
 * Step abstraction for modular clinical diagnosis workflow stages.
 *
 * <p>Enables the ClinicalWorkflowEngine to coordinate polymorphic, orderable, and potentially
 * parallel steps without embedding agent-specific invocation logic.
 */
public interface WorkflowStep {

  /**
   * Executes this workflow step using and updating the shared context.
   *
   * @param context shared clinical workflow context
   * @throws Exception if an unrecoverable failure occurs during step execution
   */
  void execute(ClinicalWorkflowContext context) throws Exception;

  /**
   * Returns the unique step identifier for observability and telemetry.
   *
   * @return step name
   */
  String stepName();

  /**
   * Returns the execution order index of this step in the sequential pipeline.
   *
   * @return integer order index
   */
  int order();
}
