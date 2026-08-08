package app.vetra.ai.agent;

import app.vetra.ai.agent.model.AgentCapability;
import app.vetra.ai.agent.model.AgentHealth;
import app.vetra.ai.agent.model.AgentRequest;
import app.vetra.ai.agent.model.AgentResponse;
import java.util.Set;

/**
 * Common contract for specialized AI agents in the Vetra platform.
 *
 * <p>Each agent encapsulates prompt ownership and domain-specific context preparation, delegating
 * execution exclusively to the underlying {@link app.vetra.ai.gateway.AIGateway}.
 */
public interface AIAgent {

  /**
   * Executes the agent workflow for the incoming request.
   *
   * @param request agent execution request
   * @return agent response containing the underlying AI response and execution metadata
   */
  AgentResponse execute(AgentRequest request);

  /**
   * Returns the unique identifier or bean name of the agent.
   *
   * @return agent name
   */
  String agentName();

  /**
   * Returns the set of capabilities supported by this agent.
   *
   * @return supported capabilities
   */
  Set<AgentCapability> supportedCapabilities();

  /**
   * Returns the operational health status of the agent.
   *
   * @return current health status
   */
  AgentHealth healthStatus();
}
