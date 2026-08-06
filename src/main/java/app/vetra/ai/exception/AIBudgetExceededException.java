package app.vetra.ai.exception;

/**
 * Thrown when an AI request exceeds token or monetary budget limits assigned to a tenant or tier.
 * Fast-fails execution.
 */
public class AIBudgetExceededException extends AIException {

  /**
   * Constructs a new AIBudgetExceededException.
   *
   * @param message the detail message
   * @param provider the provider name or budget scope
   */
  public AIBudgetExceededException(String message, String provider) {
    super(message, provider);
  }
}
