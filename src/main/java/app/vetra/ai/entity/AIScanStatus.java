package app.vetra.ai.entity;

/** Processing status lifecycle of an AI diagnostic scan. */
public enum AIScanStatus {
  PENDING,
  PROCESSING,
  COMPLETED,
  FAILED,
  VERIFIED,
  REJECTED
}
