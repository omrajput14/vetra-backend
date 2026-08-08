package app.vetra.ai.agent.gateway;

import app.vetra.ai.agent.model.AgentRequest;
import app.vetra.ai.agent.model.AgentResponse;

/**
 * Public entry point for business services to execute multi-agent workflows in the Vetra platform.
 */
public interface AgentGateway {

  /**
   * Dispatches an agent request to the appropriate healthy specialized agent.
   *
   * @param request agent execution request
   * @return agent execution response
   */
  AgentResponse execute(AgentRequest request);
}
