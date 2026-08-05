package app.vetra.ai.model;

import java.util.Map;
import java.util.Set;

/**
 * Standardized domain-agnostic request to the AIGateway.
 *
 * @param promptId the versioned prompt identifier (e.g., "diagnosis.visual.v1")
 * @param variables the context variables required by the prompt template
 * @param imageUrl the optional image URL for vision-based prompts
 * @param cacheBypass if true, bypasses the pre-flight cache evaluation
 * @param requiredCapabilities the set of capabilities the provider must support
 */
public record AIRequest(
    String promptId,
    Map<String, Object> variables,
    String imageUrl,
    boolean cacheBypass,
    Set<AICapability> requiredCapabilities) {

  /** Compact constructor that normalises null collections to empty immutable defaults. */
  public AIRequest {
    if (variables == null) {
      variables = Map.of();
    }
    if (requiredCapabilities == null) {
      requiredCapabilities = Set.of();
    }
  }
}
