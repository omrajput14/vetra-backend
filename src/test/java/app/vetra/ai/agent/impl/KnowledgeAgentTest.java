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

class KnowledgeAgentTest {

  private AIGateway aiGateway;
  private KnowledgeAgent knowledgeAgent;

  @BeforeEach
  void setUp() {
    aiGateway = mock(AIGateway.class);
    knowledgeAgent = new KnowledgeAgent(aiGateway, new AgentProperties());
  }

  @Test
  void testAgentMetadata() {
    assertEquals("KnowledgeAgent", knowledgeAgent.agentName());
    assertTrue(knowledgeAgent.supportedCapabilities().contains(AgentCapability.KNOWLEDGE));
    assertEquals(AgentHealth.HEALTHY, knowledgeAgent.healthStatus());
  }

  @Test
  void testExecuteKnowledge() {
    AIResponse mockAiResponse =
        new AIResponse("{\"disease\":\"Foot and Mouth\"}", "knowledge.disease.v1", "gemini", "gemini-1.5-flash", 10, 20, "stop");

    when(aiGateway.execute(any(AIRequest.class), any(AIExecutionContext.class)))
        .thenReturn(mockAiResponse);

    AgentRequest request =
        AgentRequest.of(
            AgentCapability.KNOWLEDGE,
            Map.of("diseaseName", "Foot and Mouth"),
            AIExecutionContext.of("tenant-1", "user-1"));

    AgentResponse response = knowledgeAgent.execute(request);

    assertNotNull(response);
    assertEquals("KnowledgeAgent", response.agentName());
    assertEquals(AgentCapability.KNOWLEDGE, response.capability());
    verify(aiGateway).execute(any(AIRequest.class), any(AIExecutionContext.class));
  }
}
