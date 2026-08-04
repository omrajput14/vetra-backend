package app.vetra.ai.exception;

/**
 * Thrown when the prompt or provider response violates safety guidelines (e.g., prompt injection
 * attempt or restricted content). This exception must not trigger a retry or fallback — the
 * violation must be surfaced directly to the caller.
 */
public class AISafetyViolationException extends AIException {

  /**
   * Constructs a new AISafetyViolationException.
   *
   * @param message the detail message
   * @param provider the provider name
   */
  public AISafetyViolationException(String message, String provider) {
    super(message, provider);
  }
}
