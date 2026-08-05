package app.vetra.ai.exception;

/**
 * Thrown when the provider rejects the request due to invalid credentials or missing API keys
 * (e.g., 401 Unauthorized). This exception should not trigger a retry or fallback — the
 * configuration must be corrected before retrying.
 */
public class AIAuthenticationException extends AIException {

  /**
   * Constructs a new AIAuthenticationException.
   *
   * @param message the detail message
   * @param provider the provider name
   */
  public AIAuthenticationException(String message, String provider) {
    super(message, provider);
  }
}
