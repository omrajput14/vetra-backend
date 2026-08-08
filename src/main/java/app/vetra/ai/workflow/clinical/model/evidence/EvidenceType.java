package app.vetra.ai.workflow.clinical.model.evidence;

/**
 * Standardized categorization of clinical evidence modalities available to the workflow.
 */
public enum EvidenceType {
  IMAGE,
  SYMPTOM,
  LAB_RESULT,
  VITAL_SIGN,
  SENSOR_OBSERVATION,
  CLINICAL_HISTORY,
  RAG_LITERATURE
}
