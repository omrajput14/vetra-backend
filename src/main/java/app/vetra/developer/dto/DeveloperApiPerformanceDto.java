package app.vetra.developer.dto;

import java.util.List;

/** Real API telemetry extracted from Micrometer http.server.requests. */
public record DeveloperApiPerformanceDto(
    long totalRequests,
    long http2xxCount,
    long http4xxCount,
    long http5xxCount,
    double errorRatePercent,
    double averageLatencyMs,
    double maxLatencyMs,
    List<EndpointMetricDto> topEndpoints,
    TelemetryClassification telemetryStatus,
    String telemetryNotice) {

  /** Individual URI route telemetry metric. */
  public record EndpointMetricDto(
      String uri,
      String method,
      String status,
      long count,
      double meanDurationMs,
      double maxDurationMs) {}
}
