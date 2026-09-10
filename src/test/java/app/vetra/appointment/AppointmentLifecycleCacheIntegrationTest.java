package app.vetra.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.vetra.animal.dto.AnimalResponse;
import app.vetra.animal.dto.CreateAnimalRequest;
import app.vetra.animal.service.AnimalService;
import app.vetra.appointment.dto.AppointmentResponse;
import app.vetra.appointment.dto.CreateAppointmentRequest;
import app.vetra.appointment.repository.AppointmentRepository;
import app.vetra.appointment.service.AppointmentService;
import app.vetra.auth.dto.FarmerRegisterRequest;
import app.vetra.auth.dto.VetRegisterRequest;
import app.vetra.auth.repository.VetProfileRepository;
import app.vetra.auth.service.AuthService;
import app.vetra.infrastructure.cache.CacheKeys;
import app.vetra.infrastructure.cache.CacheNames;
import app.vetra.infrastructure.persistence.entity.Appointment;
import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.infrastructure.persistence.enums.AnimalGender;
import app.vetra.infrastructure.persistence.enums.AppointmentStatus;
import app.vetra.infrastructure.persistence.enums.Species;
import app.vetra.infrastructure.persistence.enums.VisitType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Regression test suite proving reliable cache invalidation across appointment lifecycle methods.
 * Verifies that internal delegation (self-invocation) still reliably evicts appointment and
 * dashboard caches, preventing stale PENDING data from being returned.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(AppointmentLifecycleCacheIntegrationTest.TestCacheConfig.class)
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:vetra_cache_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
      "spring.flyway.enabled=false",
      "vetra.jwt.secret=test-jwt-secret-value-minimum-32-characters-long",
      "vetra.jwt.expiration-ms=86400000",
      "vetra.jwt.refresh-expiration-ms=604800000",
      "vetra.cors.allowed-origins=http://localhost:3000",
      "vetra.cors.allowed-methods=GET,POST,PUT,DELETE,PATCH,OPTIONS",
      "vetra.cors.allowed-headers=*",
      "vetra.cors.allow-credentials=true",
      "vetra.cors.max-age=3600",
      "vetra.aws.region=ap-south-1",
      "vetra.aws.credentials.access-key=test-key",
      "vetra.aws.credentials.secret-key=test-secret",
      "vetra.aws.s3.bucket-name=vetra-test-bucket",
      "vetra.aws.s3.presigned-url-expiry-minutes=15",
    })
class AppointmentLifecycleCacheIntegrationTest {

  @Autowired private AppointmentService appointmentService;
  @Autowired private AppointmentRepository appointmentRepository;
  @Autowired private AnimalService animalService;
  @Autowired private AuthService authService;
  @Autowired private VetProfileRepository vetProfileRepository;
  @Autowired private CacheManager cacheManager;

  private UUID animalId;
  private UUID vetProfileId;

  @TestConfiguration
  static class TestCacheConfig {
    @Bean
    @Primary
    public CacheManager testCacheManager() {
      return new ConcurrentMapCacheManager();
    }
  }

  @BeforeEach
  void setUp() {
    Cache appointmentCache = cacheManager.getCache(CacheNames.APPOINTMENTS);
    if (appointmentCache != null) {
      appointmentCache.clear();
    }
    appointmentRepository.deleteAll();

    registerUsersAndAnimal();
    VetProfile vetProfile = vetProfileRepository.findAll().get(0);
    vetProfileId = vetProfile.getId();

    CreateAnimalRequest createAnimalReq =
        new CreateAnimalRequest(
            "Daisy",
            "TAG-DAISY-" + UUID.randomUUID().toString().substring(0, 5),
            "QR-DAISY-" + UUID.randomUUID().toString().substring(0, 5),
            Species.CATTLE,
            "Jersey",
            AnimalGender.FEMALE,
            LocalDate.of(2023, 1, 15),
            null);
    AnimalResponse animal = animalService.createAnimal("farmer_cache@vetra.app", createAnimalReq);
    animalId = animal.id();
  }

  @Test
  @DisplayName("Complete appointment must evict cache so GET by ID never returns stale PENDING")
  void testCompleteAppointmentEvictsCacheAndReturnsCompleted() {
    // 1. Create appointment -> starts as PENDING
    CreateAppointmentRequest appReq =
        new CreateAppointmentRequest(
            animalId,
            vetProfileId,
            LocalDate.now().plusDays(1),
            LocalTime.of(11, 0),
            VisitType.GENERAL_CHECKUP,
            "Health checkup");

    AppointmentResponse created =
        appointmentService.createAppointment("farmer_cache@vetra.app", appReq);
    UUID apptId = created.id();
    assertEquals(AppointmentStatus.PENDING, created.status());

    // 2. GET appointment by ID -> populates cache with PENDING
    AppointmentResponse cachedFirstGet =
        appointmentService.getAppointmentById("farmer_cache@vetra.app", apptId);
    assertEquals(AppointmentStatus.PENDING, cachedFirstGet.status());

    Cache appointmentCache = cacheManager.getCache(CacheNames.APPOINTMENTS);
    assertNotNull(appointmentCache);
    String cacheKey = CacheKeys.appointmentKey(apptId);
    Cache.ValueWrapper inCache = appointmentCache.get(cacheKey);
    assertNotNull(inCache, "Appointment must be cached after initial getAppointmentById");
    assertEquals(
        AppointmentStatus.PENDING,
        ((AppointmentResponse) inCache.get()).status(),
        "Cached appointment must have PENDING status");

    // 3. Confirm appointment -> transitions to CONFIRMED and evicts cache
    appointmentService.confirmAppointment("vet_cache@vetra.app", apptId);

    // 4. completeAppointment(...) changes DB status to COMPLETED and evicts cache
    AppointmentResponse completed =
        appointmentService.completeAppointment(
            "vet_cache@vetra.app", apptId, "Consultation completed successfully.");
    assertEquals(AppointmentStatus.COMPLETED, completed.status());

    // Verify DB status is COMPLETED
    Appointment inDb = appointmentRepository.findById(apptId).orElseThrow();
    assertEquals(AppointmentStatus.COMPLETED, inDb.getStatus());

    // 5. Verify the appointment cache key is evicted
    // If the bug were present, inCacheAfterComplete would still contain PENDING!
    Cache.ValueWrapper inCacheAfterComplete = appointmentCache.get(cacheKey);
    assertNull(
        inCacheAfterComplete,
        "Appointment cache key MUST be evicted after completeAppointment()");

    // 6. A subsequent GET by ID returns COMPLETED from DB (no stale PENDING)
    AppointmentResponse freshGet =
        appointmentService.getAppointmentById("farmer_cache@vetra.app", apptId);
    assertEquals(
        AppointmentStatus.COMPLETED,
        freshGet.status(),
        "Authoritative GET after completion must return COMPLETED");

    // Verify newly cached value is COMPLETED, never PENDING
    Cache.ValueWrapper newlyCached = appointmentCache.get(cacheKey);
    assertNotNull(newlyCached);
    assertEquals(AppointmentStatus.COMPLETED, ((AppointmentResponse) newlyCached.get()).status());
  }

  @Test
  @DisplayName("Confirm appointment must evict cache so GET by ID never returns stale PENDING")
  void testConfirmAppointmentEvictsCache() {
    // 1. Create appointment -> starts as PENDING
    CreateAppointmentRequest appReq =
        new CreateAppointmentRequest(
            animalId,
            vetProfileId,
            LocalDate.now().plusDays(2),
            LocalTime.of(14, 0),
            VisitType.GENERAL_CHECKUP,
            "Pre-vaccination consultation");

    AppointmentResponse created =
        appointmentService.createAppointment("farmer_cache@vetra.app", appReq);
    UUID apptId = created.id();

    // 2. GET appointment by ID -> populates cache with PENDING
    AppointmentResponse initialGet =
        appointmentService.getAppointmentById("farmer_cache@vetra.app", apptId);
    assertEquals(AppointmentStatus.PENDING, initialGet.status());

    Cache appointmentCache = cacheManager.getCache(CacheNames.APPOINTMENTS);
    assertNotNull(appointmentCache);
    String cacheKey = CacheKeys.appointmentKey(apptId);
    assertNotNull(appointmentCache.get(cacheKey));

    // 3. Confirm appointment (self-invocation to updateStatus)
    AppointmentResponse confirmed =
        appointmentService.confirmAppointment("vet_cache@vetra.app", apptId);
    assertEquals(AppointmentStatus.CONFIRMED, confirmed.status());

    // 4. Verify cache is evicted
    assertNull(
        appointmentCache.get(cacheKey),
        "Appointment cache key MUST be evicted after confirmAppointment()");

    // 5. Subsequent GET returns CONFIRMED
    AppointmentResponse getAfterConfirm =
        appointmentService.getAppointmentById("farmer_cache@vetra.app", apptId);
    assertEquals(AppointmentStatus.CONFIRMED, getAfterConfirm.status());
  }

  private void registerUsersAndAnimal() {
    try {
      authService.registerFarmer(
          new FarmerRegisterRequest(
              "farmer_cache@vetra.app",
              "+1555111222",
              "pass123",
              "Farmer Daisy",
              "Green Pastures",
              "Village",
              "District",
              "State",
              12.0,
              56.0,
              5));
    } catch (Exception expected) {
      // User might already exist across test runs
    }

    try {
      authService.registerVet(
          new VetRegisterRequest(
              "vet_cache@vetra.app",
              "+1555333444",
              "pass123",
              "Dr. Watson",
              "VET-CACHE-1234",
              "BVSc & AH",
              "Veterinary Medicine",
              "Green Vet Clinic",
              10,
              12.1,
              56.1));
    } catch (Exception expected) {
      // Vet might already exist across test runs
    }
  }
}
