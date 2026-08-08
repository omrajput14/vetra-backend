package app.vetra.ai.workflow.clinical.step;

import app.vetra.ai.workflow.clinical.DiseaseRanker;
import app.vetra.ai.workflow.clinical.model.ClinicalWorkflowContext;
import app.vetra.ai.workflow.clinical.model.DiseaseCandidate;
import app.vetra.ai.workflow.clinical.model.WorkflowStatus;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Step 3: Merges diagnosis outputs with literature evidence, eliminates duplicate conditions,
 * and produces calibrated DiseaseCandidate rankings.
 */
@Component
public class RankingStep implements WorkflowStep {

  private static final Logger log = LoggerFactory.getLogger(RankingStep.class);
  public static final String STEP_NAME = "ranking";

  private final DiseaseRanker diseaseRanker;

  /**
   * Constructs RankingStep.
   *
   * @param diseaseRanker disease ranking and normalization engine
   */
  public RankingStep(DiseaseRanker diseaseRanker) {
    this.diseaseRanker = diseaseRanker;
  }

  @Override
  public void execute(ClinicalWorkflowContext context) throws Exception {
    long start = System.currentTimeMillis();
    log.info("Executing RankingStep for scanId={}", context.getRequest().scanId());

    try {
      List<DiseaseCandidate> ranked =
          diseaseRanker.rankDiseases(
              context.getDiagnosisResponse(),
              context.getRetrievedContext(),
              context.getRequest().symptoms());

      context.setRankedDiseases(ranked);

      long duration = System.currentTimeMillis() - start;
      context.recordStepTiming(STEP_NAME, duration);
      context.recordStepStatus(STEP_NAME, WorkflowStatus.SUCCESS);
      log.info(
          "RankingStep completed in {}ms with {} candidates for scanId={}",
          duration,
          ranked.size(),
          context.getRequest().scanId());

    } catch (Exception ex) {
      long duration = System.currentTimeMillis() - start;
      context.recordStepTiming(STEP_NAME, duration);
      context.recordStepStatus(STEP_NAME, WorkflowStatus.FAILED);
      context.addError("RankingStep failed: " + ex.getMessage());
      log.error("RankingStep failed for scanId={}: {}", context.getRequest().scanId(), ex.getMessage());
      throw ex;
    }
  }

  @Override
  public String stepName() {
    return STEP_NAME;
  }

  @Override
  public int order() {
    return 3;
  }
}
