package app.vetra.disease.entity;

/**
 * Diagnostic confidence source for epidemiological disease surveillance analytics.
 */
public enum DiagnosisConfidenceSource {
  /** Confirmed via veterinarian-approved AI diagnostic scan. */
  AI_VERIFIED,

  /** Clinical diagnosis rendered by a licensed field veterinarian. */
  VETERINARIAN,

  /** Diagnostic confirmation by an accredited diagnostic laboratory. */
  LAB_CONFIRMED,

  /** Official notification from government epidemiological health authority. */
  GOVERNMENT
}
