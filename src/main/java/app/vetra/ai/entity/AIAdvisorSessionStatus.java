package app.vetra.ai.entity;

/** Lifecycle and clinical screening state of an AI Veterinary Advisor session. */
public enum AIAdvisorSessionStatus {
  QUESTIONING,
  READY_FOR_ASSESSMENT,
  ASSESSMENT_GENERATED,
  INSUFFICIENT_INFORMATION,
  URGENT_VETERINARY_REVIEW,
  FAILED
}
