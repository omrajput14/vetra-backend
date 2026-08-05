package app.vetra.infrastructure.persistence.enums;

/** Status lifecycle of a veterinary appointment. */
public enum AppointmentStatus {
  PENDING,
  CONFIRMED,
  COMPLETED,
  CANCELLED,
  REJECTED;

  /** Checks if current state is terminal (no further transitions allowed). */
  public boolean isTerminal() {
    return this == COMPLETED || this == CANCELLED || this == REJECTED;
  }
}
