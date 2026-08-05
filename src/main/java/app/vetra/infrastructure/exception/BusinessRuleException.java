package app.vetra.infrastructure.exception;

import org.springframework.http.HttpStatus;

/** Thrown when an invalid domain state transition occurs (HTTP 422 Unprocessable Entity). */
public class BusinessRuleException extends BaseDomainException {

  /**
   * Constructs a BusinessRuleException with explicit error code.
   *
   * @param message exception message
   * @param errorCode error code from catalogue
   */
  public BusinessRuleException(String message, String errorCode) {
    super(message, errorCode, HttpStatus.UNPROCESSABLE_ENTITY);
  }

  /**
   * Constructs a BusinessRuleException with default APPT_004 code.
   *
   * @param message exception message
   */
  public BusinessRuleException(String message) {
    super(message, "APPT_004", HttpStatus.UNPROCESSABLE_ENTITY);
  }
}
