package app.vetra.ai.workflow.clinical.clinicalcase.timeline;

/** Domain event type classification for longitudinal clinical timelines. */
public enum ClinicalTimelineEventType {
  CASE_OPENED,
  ENCOUNTER_RECORDED,
  DIAGNOSIS_RECORDED,
  TREATMENT_STARTED,
  ACTION_PLAN_CREATED,
  FOLLOW_UP_SCHEDULED,
  FOLLOW_UP_COMPLETED,
  CLINICAL_STATUS_CHANGED,
  TREATMENT_RESPONSE_RECORDED,
  VETERINARIAN_REVIEW,
  REFERRAL,
  CASE_RESOLVED
}
