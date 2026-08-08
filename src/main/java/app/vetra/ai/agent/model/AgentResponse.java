package app.vetra.ai.agent.model;

import app.vetra.ai.model.AIResponse;
import java.util.Map;

/**
 * Immutable response returned by an {@link app.vetra.ai.agent.AIAgent} and the
 * {@link app.vetra.ai.agent.gateway.AgentGateway}.
 *
 * @param rawResponse standardized underlying AI response from the AI Gateway
 * @param agentName name of the executing agent bean
 * @param capability capability executed
 * @param metadata additional response metadata
 */
public record AgentResponse(
    AIResponse rawResponse,
    String agentName,
    AgentCapability capability,
    Map<String, Object> metadata) {

  /** Returns raw content from the underlying AI response. */
  public String content() {
    return rawResponse != null ? rawResponse.content() : "";
  }
}
