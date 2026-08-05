package app.vetra.ai.model;

/**
 * Standardized domain-agnostic response from the AIGateway.
 *
 * @param content the normalized textual or JSON output from the model
 * @param promptVersion the resolved prompt version used for this inference
 * @param provider the identifier of the executing AI provider
 * @param model the model alias or ID used
 * @param promptTokens the token count consumed by the input prompt
 * @param completionTokens the token count consumed by the output response
 */
public record AIResponse(
    String content,
    String promptVersion,
    String provider,
    String model,
    int promptTokens,
    int completionTokens) {

  /**
   * Returns the combined prompt and completion token count.
   *
   * @return total tokens used
   */
  public int totalTokens() {
    return promptTokens + completionTokens;
  }
}
