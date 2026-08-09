package app.vetra.ai.workflow.clinical.model.evidence;

import java.time.Instant;

/**
 * Immutable record representing telemetry from an IoT/wearable sensor (rumination collar, bolus, pedometer).
 *
 * @param sensorId sensor device identifier
 * @param sensorType sensor category (RUMINATION, ACTIVITY, BODY_TEMP, FEEDING)
 * @param reading sensor reading value
 * @param unit measurement unit
 * @param timestamp reading timestamp
 * @param status status descriptor
 */
public record SensorObservation(
    String sensorId,
    String sensorType,
    double reading,
    String unit,
    Instant timestamp,
    String status) {

  /** Canonical constructor with non-null defaults. */
  public SensorObservation {
    sensorId = sensorId != null ? sensorId.trim() : "SENSOR-UNKNOWN";
    sensorType = sensorType != null ? sensorType.trim().toUpperCase() : "GENERAL";
    unit = unit != null ? unit.trim() : "";
    timestamp = timestamp != null ? timestamp : Instant.now();
    status = status != null ? status.trim() : "OK";
  }
}
