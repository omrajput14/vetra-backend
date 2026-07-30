package app.vetra.ai.exception;

import app.vetra.infrastructure.exception.BaseDomainException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested AI provider is offline, disabled, or unconfigured.
 */
public class AIProviderUnavailableException extends BaseDomainException {

  /**
   * Constructs an AIProviderUnavailableException with custom message and code.
   *
   * @param message exception message
   * @param errorCode error code string
   */
  public AIProviderUnavailableException(String message, String errorCode) {
    super(message, errorCode, HttpStatus.SERVICE_UNAVAILABLE);
  }

  /**
   * Constructs an AIProviderUnavailableException with default message and code AI_003.
   *
   * @param message exception message
   */
  public AIProviderUnavailableException(String message) {
    super(message, "AI_003", HttpStatus.SERVICE_UNAVAILABLE);
  }
}
