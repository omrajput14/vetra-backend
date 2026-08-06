package app.vetra.ai.gateway;

import app.vetra.ai.model.AIExecutionContext;
import app.vetra.ai.model.AIRequest;
import app.vetra.ai.model.AIResponse;

/**
 * Enterprise AI Gateway serving as the single entry point for all AI interactions. It is
 * responsible for prompt resolution, variable rendering, provider routing, governance enforcement,
 * and normalizing responses from heterogeneous backend providers.
 */
public interface AIGateway {

  /**
   * Executes an AI request through the gateway using default execution context.
   *
   * @param request the generic AI request
   * @return the normalized AI response
   */
  default AIResponse execute(AIRequest request) {
    return execute(request, AIExecutionContext.empty());
  }

  /**
   * Executes an AI request through the gateway with specific execution context.
   *
   * @param request the generic AI request
   * @param context execution context carrying tenant, user, and correlation metadata
   * @return the normalized AI response
   */
  AIResponse execute(AIRequest request, AIExecutionContext context);
}
