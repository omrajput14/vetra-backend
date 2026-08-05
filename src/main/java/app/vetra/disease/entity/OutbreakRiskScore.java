package app.vetra.disease.entity;

/** Calculated epidemiological risk severity score for an outbreak cluster. */
public enum OutbreakRiskScore {
  /** Low epidemiological risk (small case count, localized). */
  LOW,

  /** Moderate epidemiological risk requiring active monitoring. */
  MEDIUM,

  /** High risk with rapid spatial velocity or high case density. */
  HIGH,

  /** Critical danger level requiring immediate quarantine protocol invocation. */
  CRITICAL
}
