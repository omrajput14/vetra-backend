package app.vetra.infrastructure.exception;

import org.springframework.http.HttpStatus;

/** Base domain exception carrying machine-readable error code and HTTP status mapping. */
public abstract class BaseDomainException extends RuntimeException {

  private final String errorCode;
  private final HttpStatus status;

  /**
   * Constructs a new BaseDomainException with message, error code, and status.
   *
   * @param message human readable message
   * @param errorCode error code from Error Catalogue
   * @param status mapped HTTP status
   */
  public BaseDomainException(String message, String errorCode, HttpStatus status) {
    super(message);
    this.errorCode = errorCode;
    this.status = status;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public HttpStatus getStatus() {
    return status;
  }
}
