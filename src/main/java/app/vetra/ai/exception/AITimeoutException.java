package app.vetra.ai.exception;

/**
 * Thrown when a provider request times out (e.g., read timeout or connection timeout). The
 * FailoverManager applies a fixed delay and retries up to the configured maximum before falling
 * back to the secondary provider.
 */
public class AITimeoutException extends AIException {

  /**
   * Constructs a new AITimeoutException.
   *
   * @param message the detail message
   * @param provider the provider name
   */
  public AITimeoutException(String message, String provider) {
    super(message, provider);
  }

  /**
   * Constructs a new AITimeoutException with a cause.
   *
   * @param message the detail message
   * @param provider the provider name
   * @param cause the underlying cause
   */
  public AITimeoutException(String message, String provider, Throwable cause) {
    super(message, provider, cause);
  }
}
