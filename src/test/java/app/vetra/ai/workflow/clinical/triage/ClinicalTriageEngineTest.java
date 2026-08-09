package app.vetra.ai.workflow.clinical.triage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.vetra.ai.agent.gateway.AgentGateway;
import app.vetra.ai.agent.model.AgentCapability;
import app.vetra.ai.agent.model.AgentRequest;
import app.vetra.ai.agent.model.AgentResponse;
import app.vetra.ai.model.AIResponse;
import app.vetra.ai.workflow.clinical.model.TriageAssessment;
import app.vetra.ai.workflow.clinical.model.TriageRequest;
import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClinicalTriageEngineTest {

  private ClinicalTriageRules triageRules;
  private AgentGateway agentGateway;
  private ClinicalTriageEngine triageEngine;

  @BeforeEach
  void setUp() {
    triageRules = new ClinicalTriageRules();
    agentGateway = mock(AgentGateway.class);
    triageEngine = new ClinicalTriageEngine(triageRules, agentGateway);
  }

  @Test
  void testEmergencyRuleTriggers_triageAgentNotCalled() {
    TriageRequest emergencyRequest =
        new TriageRequest(
            "CATTLE",
            "Holstein",
            List.of("Severe respiratory distress"),
            List.of(),
            List.of(),
            "",
            null,
            null);

    TriageAssessment assessment = triageEngine.assessTriage(emergencyRequest);

    assertNotNull(assessment);
    assertEquals(TriageUrgency.EMERGENCY, assessment.urgency());
    assertTrue(assessment.requiresImmediateVeterinaryReview());

    // Precedence guarantee verification: AgentGateway MUST NOT be invoked when deterministic rule triggers
    verifyNoInteractions(agentGateway);
  }

  @Test
  void testValidAiAssessment_accepted() throws Exception {
    TriageRequest nonEmergencyRequest =
        new TriageRequest(
            "CATTLE",
            "Jersey",
            List.of("Mild lameness"),
            List.of("Swollen joint"),
            List.of(),
            "",
            null,
            null);

    String validJson =
        """
        {
          "urgency": "PRIORITY",
          "confidence": 0.88,
          "rationale": "Non-emergency joint inflammation requiring short-term assessment.",
          "warningSigns": ["Joint swelling"],
          "recommendedActions": ["Schedule veterinary exam within 48 hours"],
          "requiresImmediateVeterinaryReview": false
        }
        """;

    AIResponse raw = new AIResponse(validJson, "1.0", "gemini", "gemini-1.5-pro", 40, 80, "STOP");
    AgentResponse resp = new AgentResponse(raw, "TriageAgent", AgentCapability.TRIAGE, Map.of());
    when(agentGateway.execute(any(AgentRequest.class))).thenReturn(resp);

    TriageAssessment assessment = triageEngine.assessTriage(nonEmergencyRequest);

    assertNotNull(assessment);
    assertEquals(TriageUrgency.PRIORITY, assessment.urgency());
    assertEquals(0.88, assessment.confidence().doubleValue(), 0.01);
    assertFalse(assessment.warningSigns().isEmpty());
  }

  @Test
  void testAiReturnsMalformedJson_conservativeUrgentFallback() {
    TriageRequest nonEmergencyRequest =
        new TriageRequest(
            "CATTLE",
            "Jersey",
            List.of("Appetite loss"),
            List.of(),
            List.of(),
            "",
            null,
            null);

    AIResponse raw = new AIResponse("NOT VALID JSON {{{", "1.0", "gemini", "gemini-1.5-pro", 40, 80, "STOP");
    AgentResponse resp = new AgentResponse(raw, "TriageAgent", AgentCapability.TRIAGE, Map.of());
    when(agentGateway.execute(any(AgentRequest.class))).thenReturn(resp);

    TriageAssessment assessment = triageEngine.assessTriage(nonEmergencyRequest);

    assertNotNull(assessment);
    assertEquals(TriageUrgency.URGENT, assessment.urgency(), "Malformed AI output must result in conservative URGENT fallback");
    assertTrue(assessment.requiresImmediateVeterinaryReview());
    assertTrue(assessment.rationale().contains("unavailable"));
  }

  @Test
  void testAiProviderFailure_conservativeUrgentFallback() {
    TriageRequest nonEmergencyRequest =
        new TriageRequest(
            "CATTLE",
            "Jersey",
            List.of("Lethargy"),
            List.of(),
            List.of(),
            "",
            null,
            null);

    when(agentGateway.execute(any(AgentRequest.class)))
        .thenThrow(new RuntimeException("Gateway provider connection timeout"));

    TriageAssessment assessment = triageEngine.assessTriage(nonEmergencyRequest);

    assertNotNull(assessment);
    assertEquals(TriageUrgency.URGENT, assessment.urgency(), "Provider failure must result in conservative URGENT fallback");
    assertTrue(assessment.requiresImmediateVeterinaryReview());
  }
}
