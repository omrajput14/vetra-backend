package app.vetra.developer.dto;

/** Telemetry source and integrity classification. */
public enum TelemetryClassification {
  REAL_BACKEND_DATA,
  DERIVED_FROM_EXISTING_DATA,
  NOT_CURRENTLY_TRACKED,
  UNAVAILABLE
}
