package app.vetra.ai.orchestrator;

import app.vetra.ai.config.AIProperties;
import app.vetra.ai.exception.AIInferenceException;
import app.vetra.ai.provider.AIInferenceResult;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Enterprise retry policy helper executing AI inference tasks with exponential backoff and
 * configurable attempts.
 */
@Component
public class AIRetryPolicy {

  private static final Logger log = LoggerFactory.getLogger(AIRetryPolicy.class);

  private final AIProperties aiProperties;

  /** Constructor injection. */
  public AIRetryPolicy(AIProperties aiProperties) {
    this.aiProperties = aiProperties;
  }

  /**
   * Executes an AI supplier action with exponential backoff retry logic.
   *
   * @param action supplier task to execute
   * @return {@link AIInferenceResult} output
   */
  public AIInferenceResult executeWithRetry(Supplier<AIInferenceResult> action) {
    int maxAttempts = aiProperties.getRetry().getMaxAttempts();
    long backoffMs = aiProperties.getRetry().getBackoff().toMillis();

    int attempt = 1;
    Throwable lastException = null;

    while (attempt <= maxAttempts) {
      try {
        log.debug("Executing AI inference attempt {}/{}", attempt, maxAttempts);
        return action.get();
      } catch (Exception ex) {
        lastException = ex;
        log.warn("AI inference attempt {}/{} failed: {}", attempt, maxAttempts, ex.getMessage());

        if (attempt == maxAttempts) {
          break;
        }

        try {
          Thread.sleep(backoffMs * attempt); // Exponential backoff multiplier
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          throw new AIInferenceException("AI inference execution interrupted", "AI_004");
        }

        attempt++;
      }
    }

    if (lastException instanceof RuntimeException rte) {
      throw rte;
    }
    throw new AIInferenceException(
        "AI inference failed after "
            + maxAttempts
            + " attempts: "
            + (lastException != null ? lastException.getMessage() : "Unknown error"),
        "AI_004");
  }
}
