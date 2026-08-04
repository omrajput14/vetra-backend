package app.vetra.ai.exception;

/**
 * Thrown when the prompt exceeds the provider's maximum context window (token limit). This
 * exception must not trigger a retry or fallback — the request must be restructured by the caller
 * before being resubmitted.
 */
public class AITokenLimitExceededException extends AIException {

  /**
   * Constructs a new AITokenLimitExceededException.
   *
   * @param message the detail message
   * @param provider the provider name
   */
  public AITokenLimitExceededException(String message, String provider) {
    super(message, provider);
  }
}
