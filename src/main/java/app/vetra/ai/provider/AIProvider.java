package app.vetra.ai.provider;

import app.vetra.ai.entity.AIProviderType;
import app.vetra.ai.model.AICapability;
import app.vetra.ai.model.AIRequest;
import app.vetra.ai.model.AIResponse;
import java.util.Set;

/**
 * Enterprise AI provider contract. Implemented by adapters for each supported LLM backend.
 * Provider adapters are responsible only for translating the standard {@link AIRequest} into the
 * provider-specific wire format and normalizing the raw response into a standard {@link AIResponse}.
 * All retry, fallover, caching, and safety logic is handled by the AIGateway layer.
 */
public interface AIProvider {

  /**
   * Checks if this provider supports the requested provider type.
   *
   * @param type the provider type to check
   * @return true if supported
   * @deprecated Use capability-based routing via ProviderRouter instead.
   */
  @Deprecated
  boolean supports(AIProviderType type);

  /**
   * Performs image analysis and returns a raw inference result.
   *
   * @param imageUrl the image URL to analyze
   * @return the inference result
   * @deprecated Use {@link #execute(AIRequest, String)} with a structured AIRequest instead.
   */
  @Deprecated
  AIInferenceResult analyze(String imageUrl);

  /**
   * Returns the enum type of this provider.
   *
   * @return the provider type
   * @deprecated Use {@link #providerName()} instead.
   */
  @Deprecated
  AIProviderType providerType();

  /**
   * Returns the active model identifier for this provider.
   *
   * @return the model name
   * @deprecated Model selection is now managed by ModelRegistry in the Gateway architecture.
   */
  @Deprecated
  String model();

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
