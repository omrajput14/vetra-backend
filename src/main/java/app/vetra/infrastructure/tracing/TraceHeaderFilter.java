package app.vetra.infrastructure.tracing;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Trace header filter.
 *
 * <p>Appends {@code X-Trace-Id} and {@code X-Span-Id} to every HTTP response when an active
 * Micrometer trace span exists. These headers allow production support engineers to locate a
 * specific request in Grafana Tempo without requiring access to server logs.
 *
 * <p>Ordering: runs after {@code RequestIdFilter} (Order 1) and {@code LoggingFilter} (Order 2),
 * so it does not interfere with existing MDC or access-log instrumentation.
 *
 * <p>Design constraints:
 *
 * <ul>
 *   <li>Only reads from the active Micrometer trace context — never generates custom IDs.
 *   <li>If no active span exists (e.g. health probes with tracing disabled), no headers are added.
 *   <li>Never exposes sensitive information — trace IDs and span IDs are non-sensitive 128/64-bit
 *       hex identifiers.
 * </ul>
 */
@Component
@Order(3)
public class TraceHeaderFilter extends OncePerRequestFilter {

  /** HTTP response header exposing the W3C trace ID for Tempo correlation. */
  public static final String HEADER_TRACE_ID = "X-Trace-Id";

  /** HTTP response header exposing the current span ID for Tempo correlation. */
  public static final String HEADER_SPAN_ID = "X-Span-Id";

  private final Tracer tracer;

  /** Constructor injection of Micrometer {@link Tracer}. */
  public TraceHeaderFilter(Tracer tracer) {
    this.tracer = tracer;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    // Read the active span from the Micrometer trace context BEFORE downstream filter execution.
    // ServerHttpObservationFilter runs before this filter (Order HIGHEST_PRECEDENCE + 1),
    // so the trace context is already populated. Adding headers before filterChain.doFilter
    // ensures headers are written to the response before HTTP body flushing commits the response.
    appendTraceHeaders(response);

    try {
      filterChain.doFilter(request, response);
    } finally {
      // Re-check in case span was created downstream (e.g. late observation startup)
      if (!response.containsHeader(HEADER_TRACE_ID)) {
        appendTraceHeaders(response);
      }
    }
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

    // Fallback to SLF4J MDC (populated automatically by Micrometer Tracing bridge)
    if ((traceId == null || traceId.isBlank()) && org.slf4j.MDC.get("traceId") != null) {
      traceId = org.slf4j.MDC.get("traceId");
      spanId = org.slf4j.MDC.get("spanId");
    }

    if (traceId != null && !traceId.isBlank() && !response.containsHeader(HEADER_TRACE_ID)) {
      response.setHeader(HEADER_TRACE_ID, traceId);
    }
    if (spanId != null && !spanId.isBlank() && !response.containsHeader(HEADER_SPAN_ID)) {
      response.setHeader(HEADER_SPAN_ID, spanId);
    }
  }
}
