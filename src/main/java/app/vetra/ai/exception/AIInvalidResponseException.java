package app.vetra.ai.exception;

/**
 * Thrown when the provider returns a response that cannot be parsed, does not match the expected
 * schema, or contains hallucinated structure. A single retry with an explicit schema instruction is
 * allowed before falling back to the secondary provider.
 */
public class AIInvalidResponseException extends AIException {

  /**
   * Constructs a new AIInvalidResponseException.
   *
   * @param message the detail message
   * @param provider the provider name
   */
  public AIInvalidResponseException(String message, String provider) {
    super(message, provider);
  }

  /**
   * Constructs a new AIInvalidResponseException with a cause.
   *
   * @param message the detail message
   * @param provider the provider name
   * @param cause the underlying cause
   */
  public AIInvalidResponseException(String message, String provider, Throwable cause) {
    super(message, provider, cause);
  }
}
