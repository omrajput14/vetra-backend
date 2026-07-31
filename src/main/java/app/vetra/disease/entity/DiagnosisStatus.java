package app.vetra.disease.entity;

/**
 * Diagnostic status of a disease report.
 */
public enum DiagnosisStatus {
  /** Initial clinical suspicion awaiting full confirmation. */
  SUSPECTED,

  /** Confirmed clinical diagnosis contributing to outbreak intelligence. */
  CONFIRMED,

  /** Rejected clinical diagnosis. */
  REJECTED
}
