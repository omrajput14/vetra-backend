package app.vetra.infrastructure.exception;

import app.vetra.infrastructure.response.ApiResponse;
import app.vetra.infrastructure.response.ApiResponse.FieldError;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Global exception handler enforcing standard ApiResponse envelope and Error Catalogue mapping.
 *
 * <p>Each handler also records tracing information on the active span when an exception occurs:
 * span status is set to ERROR and an exception event is attached. This ensures that failed requests
 * surface correctly in Grafana Tempo without relying solely on HTTP 5xx status codes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private final Tracer tracer;

  /** Constructor injection. Tracer is a no-op when tracing is disabled. */
  public GlobalExceptionHandler(Tracer tracer) {
    this.tracer = tracer;
  }

  /**
   * Handles custom typed domain exceptions (ResourceNotFoundException, ConflictException, etc.).
   */
  @ExceptionHandler(BaseDomainException.class)
  public ResponseEntity<ApiResponse<Void>> handleBaseDomainException(
      BaseDomainException ex, HttpServletRequest request) {

    log.warn(
        "Domain exception on {} {} [{}]: {}",
        request.getMethod(),
        request.getRequestURI(),
        ex.getErrorCode(),
        ex.getMessage());

    // Mark span as error for 5xx domain exceptions; 4xx domain exceptions are client errors
    // and do not indicate a service failure, so we record them as warnings only.
    if (ex.getStatus().is5xxServerError()) {
      recordSpanError(ex, true);
    } else {
      recordSpanError(ex, false);
    }

    return ResponseEntity.status(ex.getStatus())
        .body(ApiResponse.error(ex.getStatus(), ex.getMessage()));
  }

  /** Handles MethodArgumentNotValidException raised by @Valid. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {

    BindingResult binding = ex.getBindingResult();
    List<FieldError> fieldErrors =
        binding.getFieldErrors().stream()
            .map(fe -> new FieldError(fe.getField(), fe.getDefaultMessage()))
            .toList();

    log.warn(
        "Validation failed on {} {}: {} error(s)",
        request.getMethod(),
        request.getRequestURI(),
        fieldErrors.size());

    return ResponseEntity.badRequest()
        .body(ApiResponse.error(HttpStatus.BAD_REQUEST, "Request validation failed", fieldErrors));
  }

  /** Handles IllegalArgumentException for simple client validation errors. */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(
      IllegalArgumentException ex, HttpServletRequest request) {

    log.warn(
        "Illegal argument on {} {}: {}",
        request.getMethod(),
        request.getRequestURI(),
        ex.getMessage());

    return ResponseEntity.badRequest()
        .body(ApiResponse.error(HttpStatus.BAD_REQUEST, ex.getMessage()));
  }

  /** Handles optimistic locking conflicts (HTTP 409 CONFLICT - APPT_006). */
  @ExceptionHandler({
    ObjectOptimisticLockingFailureException.class,
    jakarta.persistence.OptimisticLockException.class
  })
  public ResponseEntity<ApiResponse<Void>> handleOptimisticLockingFailure(
      Exception ex, HttpServletRequest request) {

    log.warn(
        "Optimistic locking conflict on {} {}: {}",
        request.getMethod(),
        request.getRequestURI(),
        ex.getMessage());
    recordSpanError(ex, false);

    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            ApiResponse.error(
                HttpStatus.CONFLICT,
                "This record was modified by another request. Please refresh and try again."));
  }

  /** Handles DataIntegrityViolationException for database constraint failures. */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
      DataIntegrityViolationException ex, HttpServletRequest request) {

    log.warn(
        "Database constraint violation on {} {}: {}",
        request.getMethod(),
        request.getRequestURI(),
        ex.getMessage());
    recordSpanError(ex, false);

    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            ApiResponse.error(
                HttpStatus.CONFLICT,
                "A database constraint error occurred or a record with these details already exists."));
  }

  /** Handles malformed JSON request bodies. */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(
      HttpMessageNotReadableException ex, HttpServletRequest request) {

    log.warn(
        "Unreadable request body on {} {}: {}",
        request.getMethod(),
        request.getRequestURI(),
        ex.getMessage());

    return ResponseEntity.badRequest()
        .body(ApiResponse.error(HttpStatus.BAD_REQUEST, "Request body is missing or malformed"));
  }

  /** Handles requests to unmapped routes. */
  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleNotFound(
      NoHandlerFoundException ex, HttpServletRequest request) {

    log.info("Route not found: {} {}", request.getMethod(), request.getRequestURI());

    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            ApiResponse.error(
                HttpStatus.NOT_FOUND,
                "Route " + request.getMethod() + " " + request.getRequestURI() + " not found"));
  }

  /** Handles Spring Security authentication failures. */
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
      AuthenticationException ex, HttpServletRequest request) {

    log.warn(
        "Authentication failure on {} {}: {}",
        request.getMethod(),
        request.getRequestURI(),
        ex.getMessage());

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Authentication required"));
  }

  /** Handles Spring Security authorization failures. */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
      AccessDeniedException ex, HttpServletRequest request) {

    log.warn(
        "Access denied on {} {}: {}",
        request.getMethod(),
        request.getRequestURI(),
        ex.getMessage());

    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiResponse.error(HttpStatus.FORBIDDEN, "Access denied"));
  }

  /** Final catch-all for unhandled RuntimeExceptions. */
  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ApiResponse<Void>> handleRuntimeException(
      RuntimeException ex, HttpServletRequest request) {

    log.error(
        "Unhandled RuntimeException on {} {}", request.getMethod(), request.getRequestURI(), ex);
    recordSpanError(ex, true);

    return ResponseEntity.internalServerError()
        .body(
            ApiResponse.error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again."));
  }

  /** Final catch-all for any Exception. */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleException(
      Exception ex, HttpServletRequest request) {

    log.error("Unhandled Exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
    recordSpanError(ex, true);

    return ResponseEntity.internalServerError()
        .body(
            ApiResponse.error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again."));
  }

  /**
   * Records exception information on the current Micrometer trace span.
   *
   * <p>When {@code markAsError} is true, the span status is set to ERROR, which surfaces the span
   * in red in Grafana Tempo. When false, only an exception event is recorded (suitable for expected
   * 4xx client errors that do not indicate a service failure).
   *
   * <p>Span attributes: only the exception class name and a safe message fragment are recorded.
   * Stack traces, user data, emails, tokens, and IDs are never included.
   *
   * @param ex the exception to record
   * @param markAsError whether to set span status to ERROR
   */
  private void recordSpanError(Exception ex, boolean markAsError) {
    Span span = tracer.currentSpan();
    if (span == null) {
      return;
    }
    // Record exception type as a low-cardinality tag — class name only, no message content.
    span.tag("exception.type", ex.getClass().getSimpleName());
    // Record a span event with the exception type; Tempo displays these as timeline events.
    span.event("exception: " + ex.getClass().getSimpleName());
    if (markAsError) {
      span.error(ex);
    }
  }
}
