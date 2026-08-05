package app.vetra.infrastructure.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a resource conflict occurs (HTTP 409). */
public class ConflictException extends BaseDomainException {

  /**
   * Constructs a ConflictException with explicit error code.
   *
   * @param message exception message
   * @param errorCode error code from catalogue
   */
  public ConflictException(String message, String errorCode) {
    super(message, errorCode, HttpStatus.CONFLICT);
  }

  /**
   * Constructs a ConflictException with default USER_001 code.
   *
   * @param message exception message
   */
  public ConflictException(String message) {
    super(message, "USER_001", HttpStatus.CONFLICT);
  }
}
