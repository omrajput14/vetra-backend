package app.vetra.infrastructure.cache;

import java.util.UUID;

/**
 * Centralized key formatting strategies and namespace constants for Vetra Redis caching.
 * Ensures deterministic, collision-free key structures across all cache regions.
 *
 * <p>Format convention: {@code vetra:<domain>:<identifier>}
 */
public final class CacheKeys {

  /** Global application key namespace prefix. */
  public static final String PREFIX = "vetra:";

  /** Key pattern for user entities. */
  public static final String USER_KEY_PATTERN = PREFIX + "user:%s";

  /** Key pattern for user profiles. */
  public static final String USER_PROFILE_KEY_PATTERN = PREFIX + "user_profile:%s";

  /** Key pattern for animal livestock records. */
  public static final String ANIMAL_KEY_PATTERN = PREFIX + "animal:%s";

  /** Key pattern for appointments. */
  public static final String APPOINTMENT_KEY_PATTERN = PREFIX + "appointment:%s";

  /** Key pattern for medical records. */
  public static final String MEDICAL_RECORD_KEY_PATTERN = PREFIX + "medical_record:%s";

  /** Key pattern for AI diagnosis results. */
  public static final String AI_DIAGNOSIS_KEY_PATTERN = PREFIX + "ai:%s";

  /** Key pattern for disease reports. */
  public static final String DISEASE_REPORT_KEY_PATTERN = PREFIX + "disease_report:%s";

  /** Key pattern for disease outbreaks. */
  public static final String OUTBREAK_KEY_PATTERN = PREFIX + "outbreak:%s";

  /** Key pattern for farmer dashboard aggregations. */
  public static final String DASHBOARD_FARMER_KEY_PATTERN = PREFIX + "dashboard:farmer:%s";

  /** Key pattern for vet dashboard aggregations. */
  public static final String DASHBOARD_VET_KEY_PATTERN = PREFIX + "dashboard:vet:%s";

  /** Key for admin platform dashboard aggregations. */
  public static final String DASHBOARD_ADMIN_KEY = PREFIX + "dashboard:admin";

  /** Key pattern for OTP verification codes. */
  public static final String OTP_KEY_PATTERN = PREFIX + "otp:%s";

  /** Key pattern for reference data catalog. */
  public static final String REFERENCE_DATA_KEY_PATTERN = PREFIX + "ref:%s";

  private CacheKeys() {
    // Utility class private constructor
  }

  /**
   * Generates a deterministic cache key for a User entity by ID.
   *
   * @param userId unique user identifier
   * @return formatted key string
   */
  public static String userKey(UUID userId) {
    return String.format(USER_KEY_PATTERN, userId);
  }

  /**
   * Generates a deterministic cache key for a User Profile entity by ID.
   *
   * @param userId unique user identifier
   * @return formatted key string
   */
  public static String userProfileKey(UUID userId) {
    return String.format(USER_PROFILE_KEY_PATTERN, userId);
  }

  /**
   * Generates a deterministic cache key for a User Profile entity by email or phone identifier.
   *
   * @param identifier user email or phone identifier
   * @return formatted key string
   */
  public static String userProfileKey(String identifier) {
    return String.format(USER_PROFILE_KEY_PATTERN, identifier);
  }

  /**
   * Generates a deterministic cache key for an Animal entity by ID.
   *
   * @param animalId unique animal identifier
   * @return formatted key string
   */
  public static String animalKey(UUID animalId) {
    return String.format(ANIMAL_KEY_PATTERN, animalId);
  }

  /**
   * Generates a deterministic cache key for an Appointment entity by ID.
   *
   * @param appointmentId unique appointment identifier
   * @return formatted key string
   */
  public static String appointmentKey(UUID appointmentId) {
    return String.format(APPOINTMENT_KEY_PATTERN, appointmentId);
  }

  /**
   * Generates a deterministic cache key for a Medical Record entity by ID.
   *
   * @param recordId unique medical record identifier
   * @return formatted key string
   */
  public static String medicalRecordKey(UUID recordId) {
    return String.format(MEDICAL_RECORD_KEY_PATTERN, recordId);
  }

  /**
   * Generates a deterministic cache key for an AI Diagnosis result by image SHA-256 hash.
   *
   * @param imageHash SHA-256 hash of the input image
   * @return formatted key string
   */
  public static String aiDiagnosisKey(String imageHash) {
    return String.format(AI_DIAGNOSIS_KEY_PATTERN, imageHash);
  }

  /**
   * Generates a deterministic cache key for a Disease Report by ID.
   *
   * @param reportId unique disease report identifier
   * @return formatted key string
   */
  public static String diseaseReportKey(UUID reportId) {
    return String.format(DISEASE_REPORT_KEY_PATTERN, reportId);
  }

  /**
   * Generates a deterministic cache key for an Outbreak by ID.
   *
   * @param outbreakId unique outbreak identifier
   * @return formatted key string
   */
  public static String outbreakKey(UUID outbreakId) {
    return String.format(OUTBREAK_KEY_PATTERN, outbreakId);
  }

  /**
   * Generates a deterministic cache key for a Farmer Dashboard by Farmer User ID.
   *
   * @param farmerId unique farmer user identifier
   * @return formatted key string
   */
  public static String farmerDashboardKey(UUID farmerId) {
    return String.format(DASHBOARD_FARMER_KEY_PATTERN, farmerId);
  }

  /**
   * Generates a deterministic cache key for a Vet Dashboard by Vet User ID.
   *
   * @param vetId unique vet user identifier
   * @return formatted key string
   */
  public static String vetDashboardKey(UUID vetId) {
    return String.format(DASHBOARD_VET_KEY_PATTERN, vetId);
  }

  /**
   * Generates a deterministic cache key for OTP verification by phone number.
   *
   * @param phoneNumber recipient E.164 phone number
   * @return formatted key string
   */
  public static String otpKey(String phoneNumber) {
    return String.format(OTP_KEY_PATTERN, phoneNumber);
  }

  /**
   * Generates a deterministic cache key for Reference Data by category.
   *
   * @param category reference catalog category name
   * @return formatted key string
   */
  public static String referenceDataKey(String category) {
    return String.format(REFERENCE_DATA_KEY_PATTERN, category);
  }
}
