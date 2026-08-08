package app.vetra.ai.workflow.clinical.model;

/**
 * Output wrapper for the completed or partially completed Clinical Diagnosis Workflow.
 *
 * @param report synthesized veterinarian diagnosis report
 * @param context shared execution context containing all intermediate steps and timings
 * @param status workflow completion status
 * @param totalDurationMs total workflow execution time in milliseconds
 */
public record ClinicalWorkflowResult(
    ClinicalDiagnosisReport report,
    ClinicalWorkflowContext context,
    WorkflowStatus status,
    long totalDurationMs) {

  /** Canonical constructor with non-null defaults. */
  public ClinicalWorkflowResult {
    status = status != null ? status : WorkflowStatus.SUCCESS;
    if (totalDurationMs < 0) {
      totalDurationMs = 0;
    }
  }
}
