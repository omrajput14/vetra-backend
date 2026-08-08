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

class ReportAgentTest {

  private AIGateway aiGateway;
  private ReportAgent reportAgent;

  @BeforeEach
  void setUp() {
    aiGateway = mock(AIGateway.class);
    reportAgent = new ReportAgent(aiGateway, new AgentProperties());
  }

  @Test
  void testAgentMetadata() {
    assertEquals("ReportAgent", reportAgent.agentName());
    assertTrue(reportAgent.supportedCapabilities().contains(AgentCapability.REPORT));
    assertTrue(reportAgent.supportedCapabilities().contains(AgentCapability.SUMMARIZATION));
    assertEquals(AgentHealth.HEALTHY, reportAgent.healthStatus());
  }

  @Test
  void testExecuteReport() {
    AIResponse mockAiResponse =
        new AIResponse("{\"summary\":\"Clinical summary\"}", "report.summary.v1", "gemini", "gemini-1.5-flash", 10, 20, "stop");

    when(aiGateway.execute(any(AIRequest.class), any(AIExecutionContext.class)))
        .thenReturn(mockAiResponse);

    AgentRequest request =
        AgentRequest.of(
            AgentCapability.REPORT,
            Map.of("caseId", "case-123", "animalDetails", "Cattle #42"),
            AIExecutionContext.of("tenant-1", "user-1"));

    AgentResponse response = reportAgent.execute(request);

    assertNotNull(response);
    assertEquals("ReportAgent", response.agentName());
    assertEquals(AgentCapability.REPORT, response.capability());
    verify(aiGateway).execute(any(AIRequest.class), any(AIExecutionContext.class));
  }
}
