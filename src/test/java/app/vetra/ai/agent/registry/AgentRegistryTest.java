package app.vetra.ai.agent.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.vetra.ai.agent.AIAgent;
import app.vetra.ai.agent.model.AgentCapability;
import app.vetra.ai.agent.model.AgentHealth;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AgentRegistryTest {

  private AIAgent mockDiagnosisAgent;
  private AIAgent mockTreatmentAgent;
  private AgentRegistry registry;

  @BeforeEach
  void setUp() {
    mockDiagnosisAgent = mock(AIAgent.class);
    when(mockDiagnosisAgent.agentName()).thenReturn("DiagnosisAgent");
    when(mockDiagnosisAgent.supportedCapabilities())
        .thenReturn(Set.of(AgentCapability.DIAGNOSIS, AgentCapability.IMAGE_ANALYSIS));
    when(mockDiagnosisAgent.healthStatus()).thenReturn(AgentHealth.HEALTHY);

    mockTreatmentAgent = mock(AIAgent.class);
    when(mockTreatmentAgent.agentName()).thenReturn("TreatmentAgent");
    when(mockTreatmentAgent.supportedCapabilities()).thenReturn(Set.of(AgentCapability.TREATMENT));
    when(mockTreatmentAgent.healthStatus()).thenReturn(AgentHealth.HEALTHY);

    registry = new AgentRegistry(List.of(mockDiagnosisAgent, mockTreatmentAgent));
  }

  @Test
  void testFindHealthyAgent_success() {
    Optional<AIAgent> agent = registry.findHealthyAgent(AgentCapability.DIAGNOSIS);
    assertTrue(agent.isPresent());
    assertEquals("DiagnosisAgent", agent.get().agentName());

    Optional<AIAgent> imageAgent = registry.findHealthyAgent(AgentCapability.IMAGE_ANALYSIS);
    assertTrue(imageAgent.isPresent());
    assertEquals("DiagnosisAgent", imageAgent.get().agentName());

    Optional<AIAgent> treatmentAgent = registry.findHealthyAgent(AgentCapability.TREATMENT);
    assertTrue(treatmentAgent.isPresent());
    assertEquals("TreatmentAgent", treatmentAgent.get().agentName());
  }

  @Test
  void testFindHealthyAgent_unsupportedCapability() {
    Optional<AIAgent> agent = registry.findHealthyAgent(AgentCapability.REPORT);
    assertFalse(agent.isPresent());
  }

  @Test
  void testFindHealthyAgent_unhealthySkipped() {
    when(mockDiagnosisAgent.healthStatus()).thenReturn(AgentHealth.UNAVAILABLE);
    Optional<AIAgent> agent = registry.findHealthyAgent(AgentCapability.DIAGNOSIS);
    assertFalse(agent.isPresent());
  }

  @Test
  void testFindByName() {
    Optional<AIAgent> agent = registry.findByName("diagnosisagent");
    assertTrue(agent.isPresent());
    assertEquals("DiagnosisAgent", agent.get().agentName());

    assertFalse(registry.findByName("unknown").isPresent());
    assertFalse(registry.findByName(null).isPresent());
  }

  @Test
  void testEmptyDiscoveredAgentsSafety() {
    AgentRegistry emptyRegistry = new AgentRegistry(null);
    assertTrue(emptyRegistry.getAllAgents().isEmpty());
    assertFalse(emptyRegistry.findHealthyAgent(AgentCapability.DIAGNOSIS).isPresent());
  }
}
