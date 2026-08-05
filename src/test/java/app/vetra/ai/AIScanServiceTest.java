package app.vetra.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.ai.config.AIProperties;
import app.vetra.ai.dto.AIScanResponse;
import app.vetra.ai.dto.CreateAIScanRequest;
import app.vetra.ai.dto.VerifyAIScanRequest;
import app.vetra.ai.entity.AIScanStatus;
import app.vetra.ai.exception.AIProviderUnavailableException;
import app.vetra.ai.orchestrator.AIMetricsService;
import app.vetra.ai.provider.NoOpAIProvider;
import app.vetra.ai.service.AIScanService;
import app.vetra.animal.dto.AnimalResponse;
import app.vetra.animal.dto.CreateAnimalRequest;
import app.vetra.animal.service.AnimalService;
import app.vetra.auth.dto.FarmerRegisterRequest;
import app.vetra.auth.dto.VetRegisterRequest;
import app.vetra.auth.service.AuthService;
import app.vetra.infrastructure.exception.UnauthorizedResourceAccessException;
import app.vetra.infrastructure.persistence.enums.AnimalGender;
import app.vetra.infrastructure.persistence.enums.Species;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for AIScanService, AIOrchestrator, and NoOpAIProvider. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:vetra_ai_full_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
      "vetra.ai.enabled=false",
      "vetra.ai.default-provider=NONE"
    })
class AIScanServiceTest {

  @Autowired private AIScanService aiScanService;
  @Autowired private AnimalService animalService;
  @Autowired private AuthService authService;
  @Autowired private NoOpAIProvider noOpAIProvider;
  @Autowired private AIProperties aiProperties;
  @Autowired private AIMetricsService metricsService;

  @Test
  void testNoOpAIProviderBehavior() {
    assertEquals("noop", noOpAIProvider.providerName());
    assertFalse(noOpAIProvider.health());
    AIProviderUnavailableException ex =
        assertThrows(
            AIProviderUnavailableException.class,
            () -> noOpAIProvider.analyze("https://s3.amazonaws.com/vetra/sample.jpg"));
    assertEquals("noop", ex.getProvider());
  }

  @Test
  void testAIScanLifecycleAndVetVerification() {
    // 1. Register Farmer & Vet
    authService.registerFarmer(
        new FarmerRegisterRequest(
            "farmer_ai@vetra.app",
            "+1555077111",
            "pass123",
            "Farmer Bob",
            "Green Acres",
            "Village",
            "District",
            "State",
            12.0,
            56.0,
            10));

    authService.registerVet(
        new VetRegisterRequest(
            "vet_ai@vetra.app",
            "+1555077222",
            "pass123",
            "Dr. Alan",
            "VET-REG-5544",
            "BVSc",
            "Dermatology",
            "Valley Vet Clinic",
            6,
            12.1,
            56.1));

    // 2. Register Animal
    AnimalResponse animal =
        animalService.createAnimal(
            "farmer_ai@vetra.app",
            new CreateAnimalRequest(
                "Daisy",
                "TAG-AI-1",
                "QR-AI-1",
                Species.CATTLE,
                "Holstein",
                AnimalGender.FEMALE,
                LocalDate.now().minusYears(2),
                null));

    // 3. Create Scan (Farmer)
    CreateAIScanRequest scanReq =
        new CreateAIScanRequest(
            animal.id(), "https://s3.amazonaws.com/vetra/scans/daisy.jpg", "HASH-SHA256-DAISY");

    AIScanResponse createdScan = aiScanService.createScan("farmer_ai@vetra.app", scanReq);
    assertNotNull(createdScan.id());
    assertEquals(AIScanStatus.PENDING, createdScan.status());
    assertFalse(createdScan.veterinarianVerified());

    // 4. List Scans with Pageable
    Page<AIScanResponse> farmerPage =
        aiScanService.listScans("farmer_ai@vetra.app", PageRequest.of(0, 10));
    assertEquals(1, farmerPage.getTotalElements());

    // Set scan status to COMPLETED to simulate finished AI processing before vet review
    aiScanService.updateStatus(
        createdScan.id(),
        AIScanStatus.COMPLETED,
        "Initial Dermatological Observation",
        new BigDecimal("0.850"));

    // 5. Verify Scan (Farmer cannot verify -> UnauthorizedResourceAccessException)
    assertThrows(
        UnauthorizedResourceAccessException.class,
        () ->
            aiScanService.verifyScan(
                "farmer_ai@vetra.app",
                createdScan.id(),
                new VerifyAIScanRequest(true, "Looks good", null)));

    // 6. Vet Verifies Scan (Accepts Diagnosis)
    AIScanResponse verifiedScan =
        aiScanService.verifyScan(
            "vet_ai@vetra.app",
            createdScan.id(),
            new VerifyAIScanRequest(
                true, "Confirmed dermatological inflammation. Prescribed topical ointment.", null));

    assertTrue(verifiedScan.veterinarianVerified());
    assertEquals(AIScanStatus.VERIFIED, verifiedScan.status());
    assertEquals(
        "Confirmed dermatological inflammation. Prescribed topical ointment.",
        verifiedScan.notes());
  }

  @Test
  void testAIPropertiesAndMetricsTracking() {
    assertFalse(aiProperties.isEnabled());
    assertEquals(0, metricsService.getTotalRequests());
  }
}
