package app.vetra.infrastructure.cache;

import java.time.Duration;

/**
 * Centralized Time-To-Live (TTL) duration policies for Vetra enterprise cache regions. Defines
 * deterministic TTLs based on data volatility, operational SLA, and consistency rules.
 */
public final class CacheTtl {

  /** OTP verification codes: 5 minutes expiration. */
  public static final Duration OTP = Duration.ofMinutes(5);

  /** Real-time Farmer dashboard aggregations: 5 minutes expiration. */
  public static final Duration DASHBOARD_FARMER = Duration.ofMinutes(5);

  /** Real-time Vet dashboard aggregations: 5 minutes expiration. */
  public static final Duration DASHBOARD_VET = Duration.ofMinutes(5);

  /** Real-time Admin platform dashboard aggregations: 5 minutes expiration. */
  public static final Duration DASHBOARD_ADMIN = Duration.ofMinutes(5);

  /** Animal livestock records and profiles: 15 minutes expiration. */
  public static final Duration ANIMALS = Duration.ofMinutes(15);

  /** Appointment scheduling details and status: 15 minutes expiration. */
  public static final Duration APPOINTMENTS = Duration.ofMinutes(15);

  /** EVMR Medical records: 30 minutes expiration. */
  public static final Duration MEDICAL_RECORDS = Duration.ofMinutes(30);

  /** User security details and principal metadata: 30 minutes expiration. */
  public static final Duration USERS = Duration.ofMinutes(30);

  /** User profile details (Farmer/Vet metadata): 30 minutes expiration. */
  public static final Duration USER_PROFILES = Duration.ofMinutes(30);

  /** Disease surveillance reports: 1 hour expiration. */
  public static final Duration DISEASE_REPORTS = Duration.ofHours(1);

  /** Disease outbreak clusters under active surveillance: 1 hour expiration. */
  public static final Duration OUTBREAKS = Duration.ofHours(1);

  /** In-app and push notification templates and device tokens: 1 hour expiration. */
  public static final Duration NOTIFICATIONS = Duration.ofHours(1);

  /** Application settings and dynamic feature flags: 6 hours expiration. */
  public static final Duration SETTINGS = Duration.ofHours(6);

  /** Master reference data and catalog definitions: 12 hours expiration. */
  public static final Duration REFERENCE_DATA = Duration.ofHours(12);

  /** Deterministic AI multi-provider diagnosis results: 24 hours expiration. */
  public static final Duration AI_DIAGNOSIS = Duration.ofHours(24);

  /** Historical epidemiological analytics aggregations: 24 hours expiration. */
  public static final Duration ANALYTICS = Duration.ofHours(24);

  /** Default fallback cache expiration when non-specific: 30 minutes. */
  public static final Duration DEFAULT = Duration.ofMinutes(30);

  private CacheTtl() {
    // Utility class private constructor
  }
}
