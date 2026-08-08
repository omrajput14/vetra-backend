package app.vetra.ai.agent.model;

import app.vetra.ai.model.AIExecutionContext;
import java.util.Map;

/**
 * Immutable request submitted to the {@link app.vetra.ai.agent.gateway.AgentGateway}.
 *
 * @param capability requested specialized capability
 * @param inputVariables template variable substitutions
 * @param imageUrl optional image URL for vision-based reasoning
 * @param cacheBypass whether to bypass the AI cache
 * @param executionContext execution context containing tenant and user metadata
 * @param metadata additional agent-specific context parameters
 */
public record AgentRequest(
    AgentCapability capability,
    Map<String, Object> inputVariables,
    String imageUrl,
    boolean cacheBypass,
    AIExecutionContext executionContext,
    Map<String, Object> metadata) {

  /**
   * Convenience factory for creating a basic agent request.
   *
   * @param capability target capability
   * @param inputVariables variable map
   * @param executionContext context
   * @return new AgentRequest instance
   */
  public static AgentRequest of(
      AgentCapability capability,
      Map<String, Object> inputVariables,
      AIExecutionContext executionContext) {
    return new AgentRequest(
        capability,
        inputVariables != null ? Map.copyOf(inputVariables) : Map.of(),
        null,
        false,
        executionContext != null ? executionContext : AIExecutionContext.empty(),
        Map.of());
  }

  /**
   * Convenience factory for visual diagnosis requests.
   *
   * @param capability target capability
   * @param imageUrl visual artifact URL
   * @param executionContext context
   * @return new AgentRequest instance
   */
  public static AgentRequest ofVision(
      AgentCapability capability,
      String imageUrl,
      AIExecutionContext executionContext) {
    return new AgentRequest(
        capability,
        Map.of(),
        imageUrl,
        false,
        executionContext != null ? executionContext : AIExecutionContext.empty(),
        Map.of());
  }
}
