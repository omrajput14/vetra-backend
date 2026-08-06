package app.vetra.ai.prompt;

import app.vetra.ai.model.AICapability;
import java.util.Set;

/**
 * Immutable configuration for an AI prompt template and its associated generation & governance parameters.
 *
 * @param promptId the unique identifier for the prompt (e.g., "diagnosis.visual.v1")
 * @param version the prompt template version
 * @param description a human-readable description of the prompt's purpose
 * @param template the raw prompt template string containing {{variables}}
 * @param capabilities the set of capabilities a provider must support to execute this prompt
 * @param expectedFormat the expected output format from the provider (e.g., "json", "text")
 * @param temperature the desired sampling temperature for generation (e.g., 0.2)
 * @param topP the nucleus sampling parameter
 * @param maxOutputTokens the maximum number of tokens to generate
 * @param enabled whether this prompt is currently active and available for routing
 * @param safetyLevel governance safety enforcement level (e.g. "STRICT", "MODERATE", "LOW")
 * @param requiresAudit whether execution of this prompt must generate detailed audit logs
 */
@SuppressWarnings("checkstyle:ParameterNumber")
public record PromptDescriptor(
    String promptId,
    String version,
    String description,
    String template,
    Set<AICapability> capabilities,
    String expectedFormat,
    Double temperature,
    Double topP,
    Integer maxOutputTokens,
    boolean enabled,
    String safetyLevel,
    boolean requiresAudit) {

  /** Compact constructor that ensures default non-null collections and governance defaults. */
  public PromptDescriptor {
    if (capabilities == null) {
      capabilities = Set.of();
    }
    if (safetyLevel == null || safetyLevel.isBlank()) {
      safetyLevel = "STRICT";
    }
  }

  /**
   * Overloaded constructor for backward compatibility with 10-parameter callers.
   *
   * @param promptId prompt ID
   * @param version version string
   * @param description description text
   * @param template template text
   * @param capabilities capabilities set
   * @param expectedFormat format string
   * @param temperature temperature value
   * @param topP topP value
   * @param maxOutputTokens max token limit
   * @param enabled enabled status
   */
  @SuppressWarnings("checkstyle:ParameterNumber")
  public PromptDescriptor(
      String promptId,
      String version,
      String description,
      String template,
      Set<AICapability> capabilities,
      String expectedFormat,
      Double temperature,
      Double topP,
      Integer maxOutputTokens,
      boolean enabled) {
    this(
        promptId,
        version,
        description,
        template,
        capabilities,
        expectedFormat,
        temperature,
        topP,
        maxOutputTokens,
        enabled,
        "STRICT",
        true);
  }
}
