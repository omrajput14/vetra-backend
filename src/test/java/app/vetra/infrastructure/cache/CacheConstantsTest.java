package app.vetra.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Enterprise Cache Architecture Constants Tests")
class CacheConstantsTest {

  @Nested
  @DisplayName("CacheNames Constants Tests")
  class CacheNamesTests {

    @Test
    @DisplayName("Should define valid non-empty cache region names")
    void shouldDefineValidCacheNames() {
      assertThat(CacheNames.USERS).isEqualTo("users");
      assertThat(CacheNames.USER_PROFILES).isEqualTo("user_profiles");
      assertThat(CacheNames.ANIMALS).isEqualTo("animals");
      assertThat(CacheNames.APPOINTMENTS).isEqualTo("appointments");
      assertThat(CacheNames.MEDICAL_RECORDS).isEqualTo("medical_records");
      assertThat(CacheNames.AI_DIAGNOSIS).isEqualTo("ai_diagnosis");
      assertThat(CacheNames.DISEASE_REPORTS).isEqualTo("disease_reports");
      assertThat(CacheNames.OUTBREAKS).isEqualTo("outbreaks");
      assertThat(CacheNames.DASHBOARD_FARMER).isEqualTo("dashboard_farmer");
      assertThat(CacheNames.DASHBOARD_VET).isEqualTo("dashboard_vet");
      assertThat(CacheNames.DASHBOARD_ADMIN).isEqualTo("dashboard_admin");
      assertThat(CacheNames.NOTIFICATIONS).isEqualTo("notifications");
      assertThat(CacheNames.OTP).isEqualTo("otp");
      assertThat(CacheNames.SETTINGS).isEqualTo("settings");
      assertThat(CacheNames.REFERENCE_DATA).isEqualTo("reference_data");
      assertThat(CacheNames.ANALYTICS).isEqualTo("analytics");
    }
  }

  @Nested
  @DisplayName("CacheKeys Deterministic Formatting Tests")
  class CacheKeysTests {

    @Test
    @DisplayName("Should generate deterministic collision-free key strings")
    void shouldGenerateDeterministicKeys() {
      UUID sampleId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

      assertThat(CacheKeys.userKey(sampleId))
          .isEqualTo("vetra:user:123e4567-e89b-12d3-a456-426614174000");
      assertThat(CacheKeys.userProfileKey(sampleId))
          .isEqualTo("vetra:user_profile:123e4567-e89b-12d3-a456-426614174000");
      assertThat(CacheKeys.animalKey(sampleId))
          .isEqualTo("vetra:animal:123e4567-e89b-12d3-a456-426614174000");
      assertThat(CacheKeys.appointmentKey(sampleId))
          .isEqualTo("vetra:appointment:123e4567-e89b-12d3-a456-426614174000");
      assertThat(CacheKeys.medicalRecordKey(sampleId))
          .isEqualTo("vetra:medical_record:123e4567-e89b-12d3-a456-426614174000");
      assertThat(CacheKeys.diseaseReportKey(sampleId))
          .isEqualTo("vetra:disease_report:123e4567-e89b-12d3-a456-426614174000");
      assertThat(CacheKeys.outbreakKey(sampleId))
          .isEqualTo("vetra:outbreak:123e4567-e89b-12d3-a456-426614174000");
      assertThat(CacheKeys.farmerDashboardKey(sampleId))
          .isEqualTo("vetra:dashboard:farmer:123e4567-e89b-12d3-a456-426614174000");
      assertThat(CacheKeys.vetDashboardKey(sampleId))
          .isEqualTo("vetra:dashboard:vet:123e4567-e89b-12d3-a456-426614174000");
      assertThat(CacheKeys.DASHBOARD_ADMIN_KEY)
          .isEqualTo("vetra:dashboard:admin");
      assertThat(CacheKeys.otpKey("+256700000000"))
          .isEqualTo("vetra:otp:+256700000000");
      assertThat(CacheKeys.aiDiagnosisKey("a1b2c3d4e5f6"))
          .isEqualTo("vetra:ai:a1b2c3d4e5f6");
      assertThat(CacheKeys.referenceDataKey("diseases"))
          .isEqualTo("vetra:ref:diseases");
    }
  }

  @Nested
  @DisplayName("CacheTtl Policies Tests")
  class CacheTtlTests {

    @Test
    @DisplayName("Should define correct TTL durations for all regions")
    void shouldDefineCorrectTtlDurations() {
      assertThat(CacheTtl.OTP).isEqualTo(Duration.ofMinutes(5));
      assertThat(CacheTtl.DASHBOARD_FARMER).isEqualTo(Duration.ofMinutes(5));
      assertThat(CacheTtl.DASHBOARD_VET).isEqualTo(Duration.ofMinutes(5));
      assertThat(CacheTtl.DASHBOARD_ADMIN).isEqualTo(Duration.ofMinutes(5));
      assertThat(CacheTtl.ANIMALS).isEqualTo(Duration.ofMinutes(15));
      assertThat(CacheTtl.APPOINTMENTS).isEqualTo(Duration.ofMinutes(15));
      assertThat(CacheTtl.MEDICAL_RECORDS).isEqualTo(Duration.ofMinutes(30));
      assertThat(CacheTtl.USERS).isEqualTo(Duration.ofMinutes(30));
      assertThat(CacheTtl.USER_PROFILES).isEqualTo(Duration.ofMinutes(30));
      assertThat(CacheTtl.DISEASE_REPORTS).isEqualTo(Duration.ofHours(1));
      assertThat(CacheTtl.OUTBREAKS).isEqualTo(Duration.ofHours(1));
      assertThat(CacheTtl.NOTIFICATIONS).isEqualTo(Duration.ofHours(1));
      assertThat(CacheTtl.SETTINGS).isEqualTo(Duration.ofHours(6));
      assertThat(CacheTtl.REFERENCE_DATA).isEqualTo(Duration.ofHours(12));
      assertThat(CacheTtl.AI_DIAGNOSIS).isEqualTo(Duration.ofHours(24));
      assertThat(CacheTtl.ANALYTICS).isEqualTo(Duration.ofHours(24));
      assertThat(CacheTtl.DEFAULT).isEqualTo(Duration.ofMinutes(30));
    }
  }
}
