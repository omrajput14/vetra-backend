package app.vetra.ai.workflow.clinical.model;

/**
 * Standardized clinical urgency levels defining how rapidly an animal requires professional
 * veterinary assessment.
 */
public enum TriageUrgency {

  /** Immediate, emergency veterinary intervention required without delay. */
  EMERGENCY,

  /** Rapid veterinary assessment required as soon as possible (preferably within hours). */
  URGENT,

  /** Priority veterinary assessment required within a short, non-emergency timeframe. */
  PRIORITY,

  /** Routine clinical monitoring; non-urgent case based on available findings. */
  ROUTINE
}
