package app.vetra.ai.entity;

/** Processing status lifecycle of an AI diagnostic scan. */
public enum AIScanStatus {
  PENDING,
  PROCESSING,
  COMPLETED,
  FAILED,
  VERIFIED,
  /** A para-vet checked it in the field and sent it to a vet. */
  ESCALATED,
  REJECTED
}
