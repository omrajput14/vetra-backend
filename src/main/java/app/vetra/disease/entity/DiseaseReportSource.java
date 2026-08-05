package app.vetra.disease.entity;

/** Origin source of a disease surveillance report. */
public enum DiseaseReportSource {
  /** Report originating from a veterinarian-verified AI diagnostic scan. */
  AI_VERIFIED,

  /** Report submitted directly by a licensed field veterinarian. */
  VETERINARIAN,

  /** Report confirmed by laboratory diagnostic test results. */
  LAB_RESULT,

  /** Report manually submitted by an epidemiologist or authorized user. */
  MANUAL
}
