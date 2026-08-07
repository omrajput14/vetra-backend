package app.vetra.ai.provider;

import app.vetra.ai.model.AICapability;
import app.vetra.ai.model.AIRequest;
import app.vetra.ai.model.AIResponse;
import java.util.Set;

/**
 * Enterprise AI provider contract. Implemented by adapters for each supported LLM backend. Provider
 * adapters are responsible only for translating the standard {@link AIRequest} into the
 * provider-specific wire format and normalizing the raw response into a standard {@link
 * AIResponse}. All retry, fallover, caching, and safety logic is handled by the AIGateway layer.
 */
public interface AIProvider {

  /**
   * Returns the unique string identifier of this provider (e.g., "gemini", "noop").
   *
   * @return the provider name
   */
  String providerName();

  /**
   * Performs a real-time health check against the provider endpoint.
   *
   * @return true if the provider is reachable and healthy
   */
  boolean health();

  /**
   * Returns true if this provider is enabled in configuration and ready to accept requests.
   *
   * @return true if the provider is available
   */
  boolean isAvailable();

  /**
   * Executes an AI inference request using the resolved prompt text.
   *
   * @param request the standardized domain-agnostic AI request
   * @param promptText the fully resolved and validated prompt text
   * @return the standardized {@link AIResponse}
   * @throws UnsupportedOperationException if the provider has not implemented this method
   */
  default AIResponse execute(AIRequest request, String promptText) {
    throw new UnsupportedOperationException(
        "execute() is not implemented for provider: " + providerName());
  }

  /**
   * Returns the set of capabilities supported by this provider adapter. Used by the ProviderRouter
   * to match requests with compatible providers.
   *
   * @return an immutable set of supported capabilities
   */
  default Set<AICapability> supportedCapabilities() {
    return Set.of();
  }
}
