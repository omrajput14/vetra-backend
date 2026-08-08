package app.vetra.ai.workflow.clinical.triage;

import app.vetra.ai.agent.gateway.AgentGateway;
import app.vetra.ai.agent.model.AgentCapability;
import app.vetra.ai.agent.model.AgentRequest;
import app.vetra.ai.agent.model.AgentResponse;
import app.vetra.ai.workflow.clinical.model.DiseaseCandidate;
import app.vetra.ai.workflow.clinical.model.TriageAssessment;
import app.vetra.ai.workflow.clinical.model.TriageRequest;
import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 2-Layer Clinical Triage Engine.
 *
 * <p>Layer 1: Deterministic Safety Rules (precedes AI; returns EMERGENCY if critical indicators detected).
 * <p>Layer 2: AI-Assisted Reasoning via AgentGateway with strict response validation and conservative URGENT fallback.
 */
@Component
public class ClinicalTriageEngine {

  private static final Logger log = LoggerFactory.getLogger(ClinicalTriageEngine.class);

  private final ClinicalTriageRules triageRules;
  private final AgentGateway agentGateway;
  private final ObjectMapper objectMapper;

  /**
   * Constructs ClinicalTriageEngine.
   *
   * @param triageRules deterministic safety rules evaluator
   * @param agentGateway agent gateway facade
   */
  public ClinicalTriageEngine(ClinicalTriageRules triageRules, AgentGateway agentGateway) {
    this.triageRules = triageRules;
    this.agentGateway = agentGateway;
    this.objectMapper = new ObjectMapper();
  }

  /**
   * Assesses clinical urgency across Layer 1 rules and Layer 2 AI reasoning.
   *
   * @param request triage request parameters
   * @return validated {@link TriageAssessment}
   */
  public TriageAssessment assessTriage(TriageRequest request) {
    if (request == null) {
      return TriageAssessment.conservativeFallback("Null triage request");
    }

    // Layer 1 — Deterministic Safety Rules (Precedes AI evaluation)
    Optional<TriageAssessment> deterministicEmergency = triageRules.evaluateRules(request);
    if (deterministicEmergency.isPresent()) {
      log.info("ClinicalTriageEngine: Deterministic EMERGENCY rule triggered. Bypassing AI TriageAgent.");
      return deterministicEmergency.get();
    }

    // Layer 2 — AI-Assisted Triage via AgentGateway
    try {
      Map<String, Object> inputVars = buildAgentInputVariables(request);

      AgentRequest agentRequest =
          new AgentRequest(
              AgentCapability.TRIAGE,
              inputVars,
              null,
              false,
              request.executionContext(),
              request.metadata() != null ? new HashMap<>(request.metadata()) : Map.of());

      log.debug("ClinicalTriageEngine delegating to AgentGateway with capability=TRIAGE");
      AgentResponse response = agentGateway.execute(agentRequest);

      return parseAndValidateAiAssessment(response);

    } catch (Exception ex) {
      log.warn("AI TriageAgent execution or validation failed: {}. Falling back to conservative URGENT assessment.", ex.getMessage());
      return TriageAssessment.conservativeFallback(ex.getMessage());
    }
  }

  private Map<String, Object> buildAgentInputVariables(TriageRequest request) {
    Map<String, Object> vars = new HashMap<>();
    vars.put("species", request.species());
    vars.put("breed", request.breed());
    vars.put("symptoms", String.join(", ", request.symptoms()));
    vars.put("observations", String.join(", ", request.diagnosisObservations()));

    String topCondition = "Unspecified";
    if (request.rankedDiseases() != null && !request.rankedDiseases().isEmpty()) {
      DiseaseCandidate top = request.rankedDiseases().get(0);
      topCondition = top.diseaseName() + " (Confidence: " + top.confidence() + ")";
    }
    vars.put("condition", topCondition);
    vars.put("evidence", request.retrievedEvidence());
    return vars;
  }

  private TriageAssessment parseAndValidateAiAssessment(AgentResponse response) throws Exception {
    if (response == null || response.rawResponse() == null || response.rawResponse().content() == null) {
      throw new IllegalArgumentException("AgentResponse or content is null");
    }

    String json = cleanMarkdown(response.rawResponse().content());
    JsonNode node = objectMapper.readTree(json);

    TriageUrgency urgency = parseUrgency(node);
    BigDecimal confidence = parseConfidence(node);
    String rationale = parseRationale(node);
    List<String> warningSigns = extractStringList(node, "warningSigns");
    List<String> recommendedActions = extractRecommendedActions(node);

    boolean requiresImmediateReview =
        urgency == TriageUrgency.EMERGENCY || urgency == TriageUrgency.URGENT
            || (node.has("requiresImmediateVeterinaryReview") && node.get("requiresImmediateVeterinaryReview").asBoolean());

    return new TriageAssessment(
        urgency,
        confidence,
        rationale,
        warningSigns,
        recommendedActions,
        requiresImmediateReview,
        Instant.now());
  }

  private TriageUrgency parseUrgency(JsonNode node) {
    if (!node.has("urgency") || node.get("urgency").asText().isBlank()) {
      throw new IllegalArgumentException("Missing required 'urgency' field");
    }
    try {
      return TriageUrgency.valueOf(node.get("urgency").asText().trim().toUpperCase());
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid 'urgency' enum value: " + node.get("urgency").asText());
    }
  }

  private BigDecimal parseConfidence(JsonNode node) {
    if (node.has("confidence") && node.get("confidence").isNumber()) {
      double rawConf = node.get("confidence").asDouble();
      BigDecimal val = BigDecimal.valueOf(rawConf).setScale(2, RoundingMode.HALF_UP);
      if (val.compareTo(BigDecimal.ZERO) < 0) {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
      }
      if (val.compareTo(BigDecimal.ONE) > 0) {
        return BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP);
      }
      return val;
    }
    return BigDecimal.valueOf(0.50);
  }

  private String parseRationale(JsonNode node) {
    if (!node.has("rationale") || node.get("rationale").asText().isBlank()) {
      throw new IllegalArgumentException("Missing or empty 'rationale' field");
    }
    return node.get("rationale").asText().trim();
  }

  private List<String> extractRecommendedActions(JsonNode node) {
    List<String> actions = extractStringList(node, "recommendedActions");
    if (actions.isEmpty()) {
      return List.of("Contact a veterinarian for clinical advice");
    }
    return actions;
  }

  private List<String> extractStringList(JsonNode node, String fieldName) {
    List<String> list = new ArrayList<>();
    if (node.has(fieldName) && node.get(fieldName).isArray()) {
      for (JsonNode elem : node.get(fieldName)) {
        if (!elem.asText().isBlank()) {
          list.add(elem.asText().trim());
        }
      }
    }
    return list;
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
}
