package app.vetra.ai.exception;

/**
 * Thrown when an AI request violates enterprise policy rules (e.g., restricted provider/model,
 * unauthorized prompt, or forbidden parameters). Fast-fails execution.
 */
public class AIPolicyViolationException extends AIException {

  /**
   * Constructs a new AIPolicyViolationException.
   *
   * @param message the detail message
   * @param provider the provider name or policy target
   */
  public AIPolicyViolationException(String message, String provider) {
    super(message, provider);
  }
}
