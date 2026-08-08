package app.vetra.ai.agent.registry;

import app.vetra.ai.agent.AIAgent;
import app.vetra.ai.agent.model.AgentCapability;
import app.vetra.ai.agent.model.AgentHealth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Spring-managed registry for dynamic AI Agent discovery, capability indexing, and health validation.
 */
@Component
public class AgentRegistry {

  private static final Logger log = LoggerFactory.getLogger(AgentRegistry.class);

  private final Map<String, AIAgent> agentsByName = new HashMap<>();
  private final Map<AgentCapability, List<AIAgent>> agentsByCapability = new EnumMap<>(AgentCapability.class);

  /**
   * Constructs the AgentRegistry and indexes all discovered Spring agent beans.
   *
   * @param discoveredAgents list of discovered AIAgent beans
   */
  public AgentRegistry(@Autowired(required = false) List<AIAgent> discoveredAgents) {
    if (discoveredAgents != null) {
      for (AIAgent agent : discoveredAgents) {
        registerAgent(agent);
      }
    }
    log.info("AgentRegistry initialized with {} agent(s)", agentsByName.size());
  }

  private void registerAgent(AIAgent agent) {
    String name = agent.agentName().trim().toLowerCase();
    if (agentsByName.containsKey(name)) {
      log.warn("Duplicate agent name '{}' detected. Overwriting existing registration.", name);
    }
    agentsByName.put(name, agent);

    Set<AgentCapability> capabilities = agent.supportedCapabilities();
    if (capabilities != null) {
      for (AgentCapability capability : capabilities) {
        agentsByCapability
            .computeIfAbsent(capability, k -> new ArrayList<>())
            .add(agent);
      }
    }
    log.info("Registered AIAgent: name='{}', capabilities={}", name, capabilities);
  }

  /**
   * Finds the primary healthy agent supporting the requested capability.
   *
   * @param capability requested capability
   * @return healthy agent if available
   */
  public Optional<AIAgent> findHealthyAgent(AgentCapability capability) {
    if (capability == null) {
      return Optional.empty();
    }
    List<AIAgent> candidates = agentsByCapability.getOrDefault(capability, List.of());
    return candidates.stream()
        .filter(a -> a.healthStatus() == AgentHealth.HEALTHY || a.healthStatus() == AgentHealth.DEGRADED)
        .findFirst();
  }

  /**
   * Retrieves an agent by its unique name.
   *
   * @param agentName agent name
   * @return optional containing the agent if found
   */
  public Optional<AIAgent> findByName(String agentName) {
    if (agentName == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(agentsByName.get(agentName.trim().toLowerCase()));
  }

  /**
   * Returns all registered agents.
   *
   * @return unmodifiable map of name to AIAgent
   */
  public Map<String, AIAgent> getAllAgents() {
    return Collections.unmodifiableMap(agentsByName);
  }
}
