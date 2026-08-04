package app.vetra.ai.exception;

/**
 * Thrown when a requested AI provider is offline, unreachable, or returns a 5xx server error.
 * The FailoverManager applies a fixed delay and retries before routing to the secondary provider.
 */
public class AIProviderUnavailableException extends AIException {

  /**
   * Constructs a new AIProviderUnavailableException.
   *
   * @param message the detail message
   * @param provider the provider name
   */
  public AIProviderUnavailableException(String message, String provider) {
    super(message, provider);
  }

  /**
   * Constructs a new AIProviderUnavailableException with a cause.
   *
   * @param message the detail message
   * @param provider the provider name
   * @param cause the underlying cause
   */
  public AIProviderUnavailableException(String message, String provider, Throwable cause) {
    super(message, provider, cause);
  }
}
