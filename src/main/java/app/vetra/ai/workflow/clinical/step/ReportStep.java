package app.vetra.ai.workflow.clinical.step;

import app.vetra.ai.workflow.clinical.ClinicalReportBuilder;
import app.vetra.ai.workflow.clinical.model.ClinicalDiagnosisReport;
import app.vetra.ai.workflow.clinical.model.ClinicalWorkflowContext;
import app.vetra.ai.workflow.clinical.model.WorkflowStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Step 5: Pure report assembly step synthesizing all intermediate stage outputs into a veterinarian-ready ClinicalDiagnosisReport.
 */
@Component
public class ReportStep implements WorkflowStep {

  private static final Logger log = LoggerFactory.getLogger(ReportStep.class);
  public static final String STEP_NAME = "report";

  private final ClinicalReportBuilder reportBuilder;

  /**
   * Constructs ReportStep.
   *
   * @param reportBuilder pure report assembly component
   */
  public ReportStep(ClinicalReportBuilder reportBuilder) {
    this.reportBuilder = reportBuilder;
  }

  @Override
  public void execute(ClinicalWorkflowContext context) throws Exception {
    long start = System.currentTimeMillis();
    log.info("Executing ReportStep for scanId={}", context.getRequest().scanId());

    try {
      ClinicalDiagnosisReport report = reportBuilder.buildReport(context);
      context.setReport(report);
      context.setTotalDurationMs(report.totalDurationMs());

      long duration = System.currentTimeMillis() - start;
      context.recordStepTiming(STEP_NAME, duration);
      context.recordStepStatus(STEP_NAME, WorkflowStatus.SUCCESS);
      log.info("ReportStep completed in {}ms for scanId={}", duration, context.getRequest().scanId());

    } catch (Exception ex) {
      long duration = System.currentTimeMillis() - start;
      context.recordStepTiming(STEP_NAME, duration);
      context.recordStepStatus(STEP_NAME, WorkflowStatus.FAILED);
      context.addError("ReportStep failed: " + ex.getMessage());
      log.error("ReportStep failed for scanId={}: {}", context.getRequest().scanId(), ex.getMessage());
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
