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

class AdvisorAgentTest {

  private AIGateway aiGateway;
  private AgentProperties agentProperties;
  private AdvisorAgent advisorAgent;

  @BeforeEach
  void setUp() {
    aiGateway = mock(AIGateway.class);
    agentProperties = new AgentProperties();
    advisorAgent = new AdvisorAgent(aiGateway, agentProperties);
  }

  @Test
  void testAgentMetadata() {
    assertEquals("AdvisorAgent", advisorAgent.agentName());
    assertTrue(advisorAgent.supportedCapabilities().contains(AgentCapability.ADVISOR));
    assertEquals(AgentHealth.HEALTHY, advisorAgent.healthStatus());
  }

  @Test
  void testExecuteAdvisor() {
    AIResponse mockAiResponse =
        new AIResponse(
            "{\"conversationState\":\"QUESTIONING\"}",
            "diagnosis.advisor.v1",
            "noop",
            "noop-v1",
            10,
            20,
            "stop");

    when(aiGateway.execute(any(AIRequest.class), any(AIExecutionContext.class)))
        .thenReturn(mockAiResponse);

    AgentRequest request =
        AgentRequest.of(
            AgentCapability.ADVISOR,
            Map.of("latestUserMessage", "Cow not eating"),
            AIExecutionContext.empty());

    AgentResponse response = advisorAgent.execute(request);

    assertNotNull(response);
    assertEquals("AdvisorAgent", response.agentName());
    assertEquals(AgentCapability.ADVISOR, response.capability());
    assertEquals("{\"conversationState\":\"QUESTIONING\"}", response.content());
    verify(aiGateway).execute(any(AIRequest.class), any(AIExecutionContext.class));
  }
}
