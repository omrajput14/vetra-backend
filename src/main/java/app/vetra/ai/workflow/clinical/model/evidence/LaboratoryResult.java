package app.vetra.ai.workflow.clinical.model.evidence;

import java.time.Instant;

/**
 * Immutable record representing a clinical laboratory test result.
 *
 * @param testName name of the diagnostic lab test
 * @param value quantitative or qualitative result value
 * @param unit measurement unit
 * @param referenceRange expected reference range supplied with the result
 * @param status clinical abnormality status
 * @param timestamp test execution or reporting timestamp
 * @param notes additional clinical technician notes
 */
public record LaboratoryResult(
    String testName,
    String value,
    String unit,
    String referenceRange,
    AbnormalityStatus status,
    Instant timestamp,
    String notes) {

  /** Canonical constructor with non-null defaults. */
  public LaboratoryResult {
    testName = testName != null ? testName.trim() : "Unspecified Lab Test";
    value = value != null ? value.trim() : "N/A";
    unit = unit != null ? unit.trim() : "";
    referenceRange = referenceRange != null ? referenceRange.trim() : "N/A";
    status = status != null ? status : AbnormalityStatus.UNKNOWN;
    timestamp = timestamp != null ? timestamp : Instant.now();
    notes = notes != null ? notes.trim() : "";
  }
}
