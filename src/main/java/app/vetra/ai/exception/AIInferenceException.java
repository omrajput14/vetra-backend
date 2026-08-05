package app.vetra.ai.exception;

import app.vetra.infrastructure.exception.BaseDomainException;
import org.springframework.http.HttpStatus;

/** Thrown when an error occurs during AI model image analysis execution. */
public class AIInferenceException extends BaseDomainException {

  /**
   * Constructs an AIInferenceException with custom message and code.
   *
   * @param message exception message
   * @param errorCode error code string
   */
  public AIInferenceException(String message, String errorCode) {
    super(message, errorCode, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /**
   * Constructs an AIInferenceException with default message and code AI_004.
   *
   * @param message exception message
   */
  public AIInferenceException(String message) {
    super(message, "AI_004", HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
