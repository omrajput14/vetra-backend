package app.vetra.infrastructure.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a requested domain resource is not found (HTTP 404). */
public class ResourceNotFoundException extends BaseDomainException {

  /**
   * Constructs a ResourceNotFoundException with explicit error code.
   *
   * @param message exception message
   * @param errorCode error code from catalogue
   */
  public ResourceNotFoundException(String message, String errorCode) {
    super(message, errorCode, HttpStatus.NOT_FOUND);
  }

  /**
   * Constructs a ResourceNotFoundException with default SYS_007 code.
   *
   * @param message exception message
   */
  public ResourceNotFoundException(String message) {
    super(message, "SYS_007", HttpStatus.NOT_FOUND);
  }
}
