package app.vetra.disease.entity;

/**
 * Spatial-temporal velocity trend of an outbreak cluster.
 */
public enum OutbreakTrend {
  /** Case reporting velocity is accelerating. */
  INCREASING,

  /** Case reporting velocity is steady and constant. */
  STABLE,

  /** Case reporting velocity is decelerating or zero. */
  DECREASING
}
