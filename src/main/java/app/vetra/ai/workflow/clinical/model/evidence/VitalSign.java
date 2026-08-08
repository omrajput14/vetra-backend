package app.vetra.ai.workflow.clinical.model.evidence;

import java.time.Instant;

/**
 * Immutable record representing a physiological vital measurement.
 *
 * @param measurementType type of vital sign (e.g. TEMPERATURE, HEART_RATE, RESPIRATORY_RATE)
 * @param value numeric or descriptor value
 * @param unit measurement unit (e.g. Celsius, bpm)
 * @param status abnormality status
 * @param timestamp measurement timestamp
 */
public record VitalSign(
    String measurementType,
    double value,
    String unit,
    AbnormalityStatus status,
    Instant timestamp) {

  /** Canonical constructor with non-null defaults. */
  public VitalSign {
    measurementType = measurementType != null ? measurementType.trim().toUpperCase() : "UNKNOWN";
    unit = unit != null ? unit.trim() : "";
    status = status != null ? status : AbnormalityStatus.UNKNOWN;
    timestamp = timestamp != null ? timestamp : Instant.now();
  }
}
