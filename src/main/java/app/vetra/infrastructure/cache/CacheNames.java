package app.vetra.infrastructure.cache;

/**
 * Centralized constant definitions for all Spring Cache names in Vetra Backend.
 * Prevents magic strings and hardcoded cache names across the application.
 */
public final class CacheNames {

  public static final String USERS = "users";
  public static final String USER_PROFILES = "user_profiles";
  public static final String ANIMALS = "animals";
  public static final String APPOINTMENTS = "appointments";
  public static final String MEDICAL_RECORDS = "medical_records";
  public static final String AI_DIAGNOSIS = "ai_diagnosis";
  public static final String DISEASE_REPORTS = "disease_reports";
  public static final String OUTBREAKS = "outbreaks";
  public static final String DASHBOARD_FARMER = "dashboard_farmer";
  public static final String DASHBOARD_VET = "dashboard_vet";
  public static final String DASHBOARD_ADMIN = "dashboard_admin";
  public static final String NOTIFICATIONS = "notifications";
  public static final String OTP = "otp";
  public static final String SETTINGS = "settings";
  public static final String REFERENCE_DATA = "reference_data";
  public static final String ANALYTICS = "analytics";

  private CacheNames() {
    // Utility class private constructor
  }
}
