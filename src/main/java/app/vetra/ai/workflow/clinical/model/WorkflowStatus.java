package app.vetra.ai.workflow.clinical.model;

/**
 * Status lifecycle of a clinical diagnosis workflow execution.
 */
public enum WorkflowStatus {
  RUNNING,
  SUCCESS,
  PARTIAL,
  FAILED
}
