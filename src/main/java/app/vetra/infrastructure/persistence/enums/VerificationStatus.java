package app.vetra.infrastructure.persistence.enums;

/** Status lifecycle of a veterinarian professional verification. */
public enum VerificationStatus {
  PENDING,
  VERIFIED,
  REJECTED;

  /** Checks if the veterinarian profile is fully verified for public discovery. */
  public boolean isVerified() {
    return this == VERIFIED;
  }
}
