package app.vetra.ai.workflow.clinical.step;

import app.vetra.ai.agent.gateway.AgentGateway;
import app.vetra.ai.agent.model.AgentCapability;
import app.vetra.ai.agent.model.AgentRequest;
import app.vetra.ai.agent.model.AgentResponse;
import app.vetra.ai.workflow.clinical.model.ClinicalWorkflowContext;
import app.vetra.ai.workflow.clinical.model.TreatmentPlan;
import app.vetra.ai.workflow.clinical.model.TreatmentRequest;
import app.vetra.ai.workflow.clinical.model.WorkflowStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Step 4: Formulates evidence-based treatment regimens, medication protocols, and monitoring guidance via TreatmentAgent.
 */
@Component
public class TreatmentStep implements WorkflowStep {

  private static final Logger log = LoggerFactory.getLogger(TreatmentStep.class);
  public static final String STEP_NAME = "treatment";

  private final AgentGateway agentGateway;
  private final ObjectMapper objectMapper;

  /**
   * Constructs TreatmentStep.
   *
   * @param agentGateway agent gateway facade
   */
  public TreatmentStep(AgentGateway agentGateway) {
    this.agentGateway = agentGateway;
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public void execute(ClinicalWorkflowContext context) throws Exception {
    long start = System.currentTimeMillis();
    log.info("Executing TreatmentStep for scanId={}", context.getRequest().scanId());

    try {
      TreatmentRequest treatmentRequest = buildTreatmentRequest(context);
      Map<String, Object> inputVars = new HashMap<>();
      inputVars.put("condition", treatmentRequest.primaryCondition());
      inputVars.put("species", treatmentRequest.species());
      inputVars.put("breed", treatmentRequest.breed());
      inputVars.put("symptoms", String.join(", ", treatmentRequest.symptoms()));
      inputVars.put("evidence", treatmentRequest.supportingEvidence());

      AgentRequest agentRequest =
          new AgentRequest(
              AgentCapability.TREATMENT,
              inputVars,
              null,
              false,
              treatmentRequest.executionContext(),
              Map.of(
                  "scanId", context.getRequest().scanId().toString(),
                  "condition", treatmentRequest.primaryCondition()));

      AgentResponse response = agentGateway.execute(agentRequest);
      TreatmentPlan plan = parseTreatmentResponse(response, treatmentRequest.primaryCondition());
      context.setTreatmentPlan(plan);

      long duration = System.currentTimeMillis() - start;
      context.recordStepTiming(STEP_NAME, duration);
      context.recordStepStatus(STEP_NAME, WorkflowStatus.SUCCESS);
      log.info("TreatmentStep completed in {}ms for scanId={}", duration, context.getRequest().scanId());

    } catch (Exception ex) {
      long duration = System.currentTimeMillis() - start;
      context.recordStepTiming(STEP_NAME, duration);
      context.recordStepStatus(STEP_NAME, WorkflowStatus.PARTIAL);
      context.addError("TreatmentStep partial failure: " + ex.getMessage());
      log.warn("TreatmentStep partial failure for scanId={}: {}", context.getRequest().scanId(), ex.getMessage());

      String condition = extractTopConditionName(context);
      context.setTreatmentPlan(TreatmentPlan.defaultPlan(condition));
    }
  }

  /**
   * Constructs a decoupled TreatmentRequest from the shared workflow context.
   *
   * @param context shared clinical workflow context
   * @return decoupled TreatmentRequest
   */
  public TreatmentRequest buildTreatmentRequest(ClinicalWorkflowContext context) {
    String primaryCondition = extractTopConditionName(context);
    BigDecimal confidence = extractTopConfidence(context);
    String evidence = extractSupportingEvidence(context);

    return new TreatmentRequest(
        primaryCondition,
        confidence,
        context.getRequest().species(),
        context.getRequest().breed(),
        context.getRequest().symptoms(),
        evidence,
        context.getRequest().executionContext());
  }

  private String extractTopConditionName(ClinicalWorkflowContext context) {
    if (context.getRankedDiseases() != null && !context.getRankedDiseases().isEmpty()) {
      return context.getRankedDiseases().get(0).diseaseName();
    }
    return "Clinical Livestock Syndrome";
  }

  private BigDecimal extractTopConfidence(ClinicalWorkflowContext context) {
    if (context.getRankedDiseases() != null && !context.getRankedDiseases().isEmpty()) {
      return context.getRankedDiseases().get(0).confidence();
    }
    return BigDecimal.valueOf(0.50);
  }

  private String extractSupportingEvidence(ClinicalWorkflowContext context) {
    if (context.getRetrievedContext() != null && context.getRetrievedContext().contextText() != null) {
      return context.getRetrievedContext().contextText();
    }
    if (context.getRankedDiseases() != null && !context.getRankedDiseases().isEmpty()) {
      return context.getRankedDiseases().get(0).evidence();
    }
    return "Grounded clinical protocols.";
  }

  private TreatmentPlan parseTreatmentResponse(AgentResponse response, String condition) {
    if (response == null || response.rawResponse() == null) {
      return TreatmentPlan.defaultPlan(condition);
    }

    try {
      String cleanJson = cleanMarkdown(response.rawResponse().content());
      JsonNode node = objectMapper.readTree(cleanJson);

      String treatment =
          node.has("treatmentPlan") && !node.get("treatmentPlan").asText().isBlank()
              ? node.get("treatmentPlan").asText()
              : "Supportive clinical therapy and isolate animal for " + condition;

      List<String> medications = extractStringArray(node, "prescriptions");
      List<String> precautions = extractStringArray(node, "precautions");
      List<String> monitoring = extractStringArray(node, "monitoring");
      int followUp = node.has("followUpDays") ? node.get("followUpDays").asInt(3) : 3;

      return new TreatmentPlan(treatment, medications, precautions, monitoring, followUp);

    } catch (Exception e) {
      log.warn("Failed to parse structured treatment JSON, falling back to default plan: {}", e.getMessage());
      return TreatmentPlan.defaultPlan(condition);
    }
  }

  private List<String> extractStringArray(JsonNode node, String fieldName) {
    List<String> items = new ArrayList<>();
    if (node.has(fieldName) && node.get(fieldName).isArray()) {
      for (JsonNode elem : node.get(fieldName)) {
        items.add(elem.asText());
      }
    }
    return items;
  }

  private String cleanMarkdown(String text) {
    if (text == null) {
      return "{}";
    }
    String cleaned = text.trim();
    if (cleaned.startsWith("```json")) {
      cleaned = cleaned.substring(7);
    } else if (cleaned.startsWith("```")) {
      cleaned = cleaned.substring(3);
    }
    if (cleaned.endsWith("```")) {
      cleaned = cleaned.substring(0, cleaned.length() - 3);
    }
    return cleaned.trim();
  }

  @Override
  public String stepName() {
    return STEP_NAME;
  }

  @Override
  public int order() {
    return 6;
  }
}
