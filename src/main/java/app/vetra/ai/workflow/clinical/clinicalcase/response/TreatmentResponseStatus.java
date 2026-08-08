package app.vetra.ai.workflow.clinical.clinicalcase.response;

/** Deterministic treatment response classification. */
public enum TreatmentResponseStatus {
  IMPROVING,
  STABLE,
  WORSENING,
  NO_MEASURABLE_CHANGE,
  INSUFFICIENT_DATA
}
