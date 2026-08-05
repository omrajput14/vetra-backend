package app.vetra.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Custom AuthenticationEntryPoint serializing standardized Error Catalogue envelopes for
 * unauthenticated / expired JWT requests.
 */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final io.micrometer.tracing.Tracer tracer;

  /** Constructor injection. */
  public CustomAuthenticationEntryPoint(io.micrometer.tracing.Tracer tracer) {
    this.tracer = tracer;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException, ServletException {

    // Attach trace headers before writing response body to committed output stream
    appendTraceHeaders(response);

    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    String authHeader = request.getHeader("Authorization");
    String errorCode = "AUTH_003";
    String errorMessage = "Authentication token is invalid or missing.";

    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      errorCode = "AUTH_002";
      errorMessage = "Your session has expired. Please log in again.";
    }

    Map<String, Object> errorObj = new LinkedHashMap<>();
    errorObj.put("code", errorCode);
    errorObj.put("message", errorMessage);
    errorObj.put("details", null);
    errorObj.put("timestamp", Instant.now().toString());
    errorObj.put("path", request.getRequestURI());

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("success", false);
    body.put("status", HttpStatus.UNAUTHORIZED.value());
    body.put("message", errorMessage);
    body.put("data", null);
    body.put("error", errorObj);
    body.put("timestamp", Instant.now().toString());

    objectMapper.writeValue(response.getOutputStream(), body);
  }

  private void appendTraceHeaders(HttpServletResponse response) {
    String traceId = null;
    String spanId = null;

    if (tracer.currentTraceContext() != null && tracer.currentTraceContext().context() != null) {
      traceId = tracer.currentTraceContext().context().traceId();
      spanId = tracer.currentTraceContext().context().spanId();
    } else if (tracer.currentSpan() != null && tracer.currentSpan().context() != null) {
      traceId = tracer.currentSpan().context().traceId();
      spanId = tracer.currentSpan().context().spanId();
    }

    if ((traceId == null || traceId.isBlank()) && org.slf4j.MDC.get("traceId") != null) {
      traceId = org.slf4j.MDC.get("traceId");
      spanId = org.slf4j.MDC.get("spanId");
    }

    if (traceId != null && !traceId.isBlank() && !response.containsHeader("X-Trace-Id")) {
      response.setHeader("X-Trace-Id", traceId);
    }
    if (spanId != null && !spanId.isBlank() && !response.containsHeader("X-Span-Id")) {
      response.setHeader("X-Span-Id", spanId);
    }
  }
}
