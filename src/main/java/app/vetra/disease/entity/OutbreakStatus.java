package app.vetra.disease.entity;

/** Status lifecycle of a disease outbreak cluster. */
public enum OutbreakStatus {
  /** Newly detected cluster awaiting initial field investigation. */
  DETECTED,

  /** Active disease outbreak requiring active containment procedures. */
  ACTIVE,

  /** Outbreak under active surveillance and monitoring. */
  MONITORING,

  /** Outbreak successfully contained and resolved. */
  RESOLVED
}
