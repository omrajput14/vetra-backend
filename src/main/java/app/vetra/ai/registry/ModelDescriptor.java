package app.vetra.ai.registry;

import app.vetra.ai.model.AICapability;
import java.util.Set;

/**
 * Describes a registered AI model with its capabilities and constraints. Model descriptors are
 * immutable and constructed from {@link app.vetra.ai.config.AIGatewayProperties.ModelConfig}
 * during application startup.
 *
 * @param alias the unique model alias used by business logic (e.g., "diagnostics-fast")
 * @param modelId the provider-specific model identifier (e.g., "gemini-2.5-flash")
 * @param providerName the name of the provider that hosts this model
 * @param capabilities the set of capabilities this model supports
 * @param contextWindow the maximum number of input tokens
 * @param maxOutputTokens the maximum number of output tokens
 * @param enabled whether this model is available for routing
 */
public record ModelDescriptor(
    String alias,
    String modelId,
    String providerName,
    Set<AICapability> capabilities,
    int contextWindow,
    int maxOutputTokens,
    boolean enabled) {

  /**
   * Returns true if this model supports all of the requested capabilities.
   *
   * @param required the set of capabilities that must be supported
   * @return true if all required capabilities are present
   */
  public boolean supports(Set<AICapability> required) {
    return capabilities.containsAll(required);
  }
}
