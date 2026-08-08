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
import app.vetra.ai.model.AIResponse;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TriageAgentTest {

  private AIGateway aiGateway;
  private AgentProperties agentProperties;
  private TriageAgent triageAgent;

  @BeforeEach
  void setUp() {
    aiGateway = mock(AIGateway.class);
    agentProperties = new AgentProperties();
    triageAgent = new TriageAgent(aiGateway, agentProperties);
  }

  @Test
  void testAgentProperties() {
    assertEquals("TriageAgent", triageAgent.agentName());
    assertTrue(triageAgent.supportedCapabilities().contains(AgentCapability.TRIAGE));
    assertEquals(AgentHealth.HEALTHY, triageAgent.healthStatus());
  }

  @Test
  void testExecute_delegatesToAiGateway() {
    AgentRequest request =
        new AgentRequest(
            AgentCapability.TRIAGE,
            Map.of("species", "BOVINE"),
            null,
            false,
            null,
            Map.of("scanId", "123"));

    AIResponse mockResponse =
        new AIResponse(
            "{\"urgency\":\"URGENT\"}",
            "1.0",
            "gemini",
            "gemini-1.5-pro",
            40,
            80,
            "STOP");

    when(aiGateway.execute(any(), any())).thenReturn(mockResponse);

    AgentResponse response = triageAgent.execute(request);

    assertNotNull(response);
    assertEquals("TriageAgent", response.agentName());
    assertEquals(AgentCapability.TRIAGE, response.capability());
    verify(aiGateway).execute(any(), any());
  }
}
