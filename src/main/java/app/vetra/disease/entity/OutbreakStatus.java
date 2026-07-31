package app.vetra.disease.entity;

/**
 * Status lifecycle of a disease outbreak.
 */
public enum OutbreakStatus {
  /** Active disease outbreak requiring containment procedures. */
  ACTIVE,

  /** Outbreak under active surveillance and monitoring. */
  MONITORING,

  /** Outbreak successfully contained and resolved. */
  RESOLVED
}
