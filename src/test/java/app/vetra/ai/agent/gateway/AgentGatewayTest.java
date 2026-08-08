package app.vetra.ai.agent.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.vetra.ai.agent.AIAgent;
import app.vetra.ai.agent.model.AgentCapability;
import app.vetra.ai.agent.model.AgentHealth;
import app.vetra.ai.agent.model.AgentRequest;
import app.vetra.ai.agent.model.AgentResponse;
import app.vetra.ai.agent.registry.AgentRegistry;
import app.vetra.ai.exception.AIConfigurationException;
import app.vetra.ai.model.AIExecutionContext;
import app.vetra.ai.model.AIResponse;
import app.vetra.ai.observability.AIMetricsCollector;
import app.vetra.ai.observability.AIObservationConvention;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AgentGatewayTest {

  private AIAgent mockAgent;
  private AgentRegistry agentRegistry;
  private AIMetricsCollector metricsCollector;
  private AIObservationConvention observationConvention;
  private DefaultAgentGateway agentGateway;

  @BeforeEach
  void setUp() {
    mockAgent = mock(AIAgent.class);
    when(mockAgent.agentName()).thenReturn("DiagnosisAgent");
    when(mockAgent.supportedCapabilities()).thenReturn(Set.of(AgentCapability.DIAGNOSIS));
    when(mockAgent.healthStatus()).thenReturn(AgentHealth.HEALTHY);

    agentRegistry = new AgentRegistry(List.of(mockAgent));
    metricsCollector = new AIMetricsCollector(new SimpleMeterRegistry());
    observationConvention = mock(AIObservationConvention.class);

    agentGateway = new DefaultAgentGateway(agentRegistry, metricsCollector, observationConvention);
  }

  @Test
  void testExecute_success() {
    AIResponse mockAiResponse =
        new AIResponse("result", "v1", "gemini", "gemini-1.5-flash", 10, 20, "stop");
    AgentResponse mockResponse =
        new AgentResponse(mockAiResponse, "DiagnosisAgent", AgentCapability.DIAGNOSIS, Map.of());

    when(mockAgent.execute(any(AgentRequest.class))).thenReturn(mockResponse);

    AgentRequest request =
        AgentRequest.of(
            AgentCapability.DIAGNOSIS,
            Map.of(),
            AIExecutionContext.of("tenant-1", "user-1"));

    AgentResponse response = agentGateway.execute(request);

    assertNotNull(response);
    assertEquals("DiagnosisAgent", response.agentName());
    assertEquals(AgentCapability.DIAGNOSIS, response.capability());
    verify(mockAgent).execute(any(AgentRequest.class));
    verify(observationConvention).recordSpanEvent("ai.agent.selected");
  }

  @Test
  void testExecute_unsupportedCapabilityThrows() {
    AgentRequest request =
        AgentRequest.of(
            AgentCapability.TREATMENT,
            Map.of(),
            AIExecutionContext.of("tenant-1", "user-1"));

    AIConfigurationException ex =
        assertThrows(AIConfigurationException.class, () -> agentGateway.execute(request));
    assertEquals("AGENT_UNAVAILABLE", ex.getErrorCode());
  }

  @Test
  void testExecute_nullRequestThrows() {
    assertThrows(AIConfigurationException.class, () -> agentGateway.execute(null));
  }
}
