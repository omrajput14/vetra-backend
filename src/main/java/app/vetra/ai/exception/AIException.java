package app.vetra.ai.exception;

/** Base exception for all AI Gateway errors. */
public abstract class AIException extends RuntimeException {

  private final String provider;

  /**
   * Constructs a new AIException with a message and provider.
   *
   * @param message the detail message
   * @param provider the provider name
   */
  public AIException(String message, String provider) {
    super(message);
    this.provider = provider;
  }

  /**
   * Constructs a new AIException with a message, provider, and cause.
   *
   * @param message the detail message
   * @param provider the provider name
   * @param cause the underlying cause
   */
  public AIException(String message, String provider, Throwable cause) {
    super(message, cause);
    this.provider = provider;
  }

  /**
   * Gets the provider name associated with this exception.
   *
   * @return the provider name
   */
  public String getProvider() {
    return provider;
  }
}
