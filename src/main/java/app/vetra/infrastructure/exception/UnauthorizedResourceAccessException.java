package app.vetra.infrastructure.exception;

import org.springframework.http.HttpStatus;

/** Thrown when an authenticated user attempts to access a resource they do not own (HTTP 403). */
public class UnauthorizedResourceAccessException extends BaseDomainException {

  /**
   * Constructs an UnauthorizedResourceAccessException with explicit error code.
   *
   * @param message exception message
   * @param errorCode error code from catalogue
   */
  public UnauthorizedResourceAccessException(String message, String errorCode) {
    super(message, errorCode, HttpStatus.FORBIDDEN);
  }

  /**
   * Constructs an UnauthorizedResourceAccessException with default AUTH_006 code.
   *
   * @param message exception message
   */
  public UnauthorizedResourceAccessException(String message) {
    super(message, "AUTH_006", HttpStatus.FORBIDDEN);
  }
}
