package app.vetra.ai.observability;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AIObservationConventionTest {

  private Tracer tracer;
  private Span span;
  private AIObservationConvention convention;

  @BeforeEach
  void setUp() {
    tracer = mock(Tracer.class);
    span = mock(Span.class);
    when(tracer.currentSpan()).thenReturn(span);
    convention = new AIObservationConvention(tracer);
  }

  @Test
  void testTagCurrentSpan() {
    convention.tagCurrentSpan("gemini", "gemini-1.5-flash", "test.v1", "v1", "success");

    verify(span).tag("provider", "gemini");
    verify(span).tag("model", "gemini-1.5-flash");
    verify(span).tag("prompt_id", "test.v1");
    verify(span).tag("prompt_version", "v1");
    verify(span).tag("status", "success");
  }

  @Test
  void testRecordSpanEvent() {
    convention.recordSpanEvent("ai.cache.hit");
    verify(span).event("ai.cache.hit");
  }

  @Test
  void testNullTracerSafety() {
    AIObservationConvention safeConvention = new AIObservationConvention(null);
    // Should not throw NullPointerException
    safeConvention.tagCurrentSpan("noop", "noop-v1", "test.v1", "v1", "success");
    safeConvention.recordSpanEvent("ai.cache.miss");
  }
}
