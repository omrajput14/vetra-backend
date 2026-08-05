package app.vetra.ai.exception;

import app.vetra.infrastructure.exception.BaseDomainException;
import org.springframework.http.HttpStatus;

/** Thrown when AI configuration properties or parameters are invalid or missing. */
public class AIConfigurationException extends BaseDomainException {

  /**
   * Constructs an AIConfigurationException with custom message and code.
   *
   * @param message exception message
   * @param errorCode error code string
   */
  public AIConfigurationException(String message, String errorCode) {
    super(message, errorCode, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /**
   * Constructs an AIConfigurationException with default message and code AI_005.
   *
   * @param message exception message
   */
  public AIConfigurationException(String message) {
    super(message, "AI_005", HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
