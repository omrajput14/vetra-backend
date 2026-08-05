package app.vetra.ai.exception;

/**
 * Thrown when the provider returns a 429 Too Many Requests or quota exceeded response. The
 * FailoverManager applies exponential backoff before routing to a secondary provider.
 */
public class AIRateLimitException extends AIException {

  /**
   * Constructs a new AIRateLimitException.
   *
   * @param message the detail message
   * @param provider the provider name
   */
  public AIRateLimitException(String message, String provider) {
    super(message, provider);
  }
}
