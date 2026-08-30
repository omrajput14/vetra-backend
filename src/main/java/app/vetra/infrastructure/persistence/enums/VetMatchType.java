package app.vetra.infrastructure.persistence.enums;

/**
 * Categorizes the proximity and matching criteria used to discover veterinarians.
 */
public enum VetMatchType {
  /** Matched via haversine GPS coordinate distance calculation within radius. */
  GPS_RADIUS,

  /** Matched via exact normalized village name fallback. */
  SAME_VILLAGE,

  /** Matched via exact normalized taluka / sub-district name fallback. */
  SAME_TALUKA,

  /** Matched via exact normalized district name fallback. */
  SAME_DISTRICT,

  /** General directory listing when no specific geographic filter matched or requested. */
  GENERAL
}
