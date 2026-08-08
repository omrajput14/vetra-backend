package app.vetra.ai.workflow.clinical.step;

import app.vetra.ai.agent.gateway.AgentGateway;
import app.vetra.ai.agent.model.AgentCapability;
import app.vetra.ai.agent.model.AgentRequest;
import app.vetra.ai.agent.model.AgentResponse;
import app.vetra.ai.workflow.clinical.model.ClinicalWorkflowContext;
import app.vetra.ai.workflow.clinical.model.WorkflowStatus;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Step 1: Visual livestock pathology analysis and clinical anomaly detection via DiagnosisAgent.
 */
@Component
public class DiagnosisStep implements WorkflowStep {

  private static final Logger log = LoggerFactory.getLogger(DiagnosisStep.class);
  public static final String STEP_NAME = "diagnosis";

  private final AgentGateway agentGateway;

  /**
   * Constructs DiagnosisStep.
   *
   * @param agentGateway agent gateway facade
   */
  public DiagnosisStep(AgentGateway agentGateway) {
    this.agentGateway = agentGateway;
  }

  @Override
  public void execute(ClinicalWorkflowContext context) throws Exception {
    long start = System.currentTimeMillis();
    log.info("Executing DiagnosisStep for scanId={}", context.getRequest().scanId());

    try {
      String imageUrl = context.getRequest().imageUrl();
      Map<String, Object> inputVars = Map.of(
          "species", context.getRequest().species(),
          "breed", context.getRequest().breed(),
          "symptoms", String.join(", ", context.getRequest().symptoms())
      );

      AgentRequest agentRequest =
          new AgentRequest(
              AgentCapability.DIAGNOSIS,
              inputVars,
              imageUrl,
              false,
              context.getRequest().executionContext(),
              Map.of("scanId", context.getRequest().scanId().toString()));

      AgentResponse response = agentGateway.execute(agentRequest);
      context.setDiagnosisResponse(response);

      long duration = System.currentTimeMillis() - start;
      context.recordStepTiming(STEP_NAME, duration);
      context.recordStepStatus(STEP_NAME, WorkflowStatus.SUCCESS);
      log.info("DiagnosisStep completed in {}ms for scanId={}", duration, context.getRequest().scanId());

    } catch (Exception ex) {
      long duration = System.currentTimeMillis() - start;
      context.recordStepTiming(STEP_NAME, duration);
      context.recordStepStatus(STEP_NAME, WorkflowStatus.FAILED);
      context.addError("DiagnosisStep failed: " + ex.getMessage());
      log.error("DiagnosisStep failed for scanId={}: {}", context.getRequest().scanId(), ex.getMessage());
      throw ex;
    }
  }

  @Override
  public String stepName() {
    return STEP_NAME;
  }

  @Override
  public int order() {
    return 1;
  }
}
