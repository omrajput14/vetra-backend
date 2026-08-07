package app.vetra.ai.observability;

import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Enterprise OpenTelemetry observation convention helper for the AI Gateway.
 *
 * <p>Enriches active OpenTelemetry spans with low-cardinality AI tags (provider, model, promptId,
 * promptVersion, status) and records key operational span events without creating duplicate root
 * spans or parallel instrumentation.
 */
@Component
public class AIObservationConvention {

  private final Tracer tracer;

  /**
   * Constructs AIObservationConvention.
   *
   * @param tracer Micrometer Tracer instance (optional-injected)
   */
  public AIObservationConvention(@Autowired(required = false) Tracer tracer) {
    this.tracer = tracer;
  }

  /**
   * Attaches low-cardinality AI execution metadata tags to the current active span.
   *
   * @param provider provider identifier
   * @param model model alias or ID
   * @param promptId prompt template ID
   * @param promptVersion prompt template version
   * @param status execution status
   */
  public void tagCurrentSpan(
      String provider, String model, String promptId, String promptVersion, String status) {
    if (tracer != null && tracer.currentSpan() != null) {
      if (provider != null) {
        tracer.currentSpan().tag(AIDashboardMetadata.TAG_PROVIDER, provider.toLowerCase());
      }
      if (model != null) {
        tracer.currentSpan().tag(AIDashboardMetadata.TAG_MODEL, model.toLowerCase());
      }
      if (promptId != null) {
        tracer.currentSpan().tag(AIDashboardMetadata.TAG_PROMPT_ID, promptId);
      }
      if (promptVersion != null) {
        tracer.currentSpan().tag(AIDashboardMetadata.TAG_PROMPT_VERSION, promptVersion);
      }
      if (status != null) {
        tracer.currentSpan().tag(AIDashboardMetadata.TAG_STATUS, status.toLowerCase());
      }
    }
  }

  /**
   * Records a span event on the current active span.
   *
   * @param eventName event name (e.g. ai.cache.hit, ai.failover)
   */
  public void recordSpanEvent(String eventName) {
    if (tracer != null && tracer.currentSpan() != null && eventName != null) {
      tracer.currentSpan().event(eventName);
    }
  }
}
