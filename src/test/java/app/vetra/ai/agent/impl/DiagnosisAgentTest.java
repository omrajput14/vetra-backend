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

class DiagnosisAgentTest {

  private AIGateway aiGateway;
  private AgentProperties agentProperties;
  private DiagnosisAgent diagnosisAgent;

  @BeforeEach
  void setUp() {
    aiGateway = mock(AIGateway.class);
    agentProperties = new AgentProperties();
    diagnosisAgent = new DiagnosisAgent(aiGateway, agentProperties);
  }

  @Test
  void testAgentMetadata() {
    assertEquals("DiagnosisAgent", diagnosisAgent.agentName());
    assertTrue(diagnosisAgent.supportedCapabilities().contains(AgentCapability.DIAGNOSIS));
    assertTrue(diagnosisAgent.supportedCapabilities().contains(AgentCapability.IMAGE_ANALYSIS));
    assertEquals(AgentHealth.HEALTHY, diagnosisAgent.healthStatus());
  }

  @Test
  void testExecuteDiagnosis() {
    AIResponse mockAiResponse =
        new AIResponse("{\"condition\":\"Mastitis\"}", "diagnosis.visual.v1", "gemini", "gemini-1.5-flash", 10, 20, "stop");

    when(aiGateway.execute(any(AIRequest.class), any(AIExecutionContext.class)))
        .thenReturn(mockAiResponse);

    AgentRequest request =
        AgentRequest.ofVision(
            AgentCapability.DIAGNOSIS,
            "https://vetra.app/img.jpg",
            AIExecutionContext.of("tenant-1", "user-1"));

    AgentResponse response = diagnosisAgent.execute(request);

    assertNotNull(response);
    assertEquals("DiagnosisAgent", response.agentName());
    assertEquals(AgentCapability.DIAGNOSIS, response.capability());
    assertEquals("{\"condition\":\"Mastitis\"}", response.content());
    verify(aiGateway).execute(any(AIRequest.class), any(AIExecutionContext.class));
  }
}
