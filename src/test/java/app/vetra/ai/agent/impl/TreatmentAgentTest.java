package app.vetra.ai.agent.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.vetra.ai.agent.model.AgentCapability;
import app.vetra.ai.agent.model.AgentHealth;
import app.vetra.ai.agent.model.AgentRequest;
import app.vetra.ai.agent.model.AgentResponse;
import app.vetra.ai.config.AgentProperties;
import app.vetra.ai.gateway.AIGateway;
import app.vetra.ai.model.AIExecutionContext;
import app.vetra.ai.model.AIRequest;
import app.vetra.ai.model.AIResponse;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TreatmentAgentTest {

  private AIGateway aiGateway;
  private TreatmentAgent treatmentAgent;

  @BeforeEach
  void setUp() {
    aiGateway = mock(AIGateway.class);
    treatmentAgent = new TreatmentAgent(aiGateway, new AgentProperties());
  }

  @Test
  void testAgentMetadata() {
    assertEquals("TreatmentAgent", treatmentAgent.agentName());
    assertTrue(treatmentAgent.supportedCapabilities().contains(AgentCapability.TREATMENT));
    assertEquals(AgentHealth.HEALTHY, treatmentAgent.healthStatus());
  }

  @Test
  void testExecuteTreatment() {
    AIResponse mockAiResponse =
        new AIResponse("{\"dosageGuidance\":\"Twice daily\"}", "treatment.recommendation.v1", "gemini", "gemini-1.5-flash", 10, 20, "stop");

    when(aiGateway.execute(any(AIRequest.class), any(AIExecutionContext.class)))
        .thenReturn(mockAiResponse);

    AgentRequest request =
        AgentRequest.of(
            AgentCapability.TREATMENT,
            Map.of("condition", "Mastitis", "species", "CATTLE"),
            AIExecutionContext.of("tenant-1", "user-1"));

    AgentResponse response = treatmentAgent.execute(request);

    assertNotNull(response);
    assertEquals("TreatmentAgent", response.agentName());
    assertEquals(AgentCapability.TREATMENT, response.capability());
    verify(aiGateway).execute(any(AIRequest.class), any(AIExecutionContext.class));
  }
}
