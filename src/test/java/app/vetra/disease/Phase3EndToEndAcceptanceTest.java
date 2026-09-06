package app.vetra.disease;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.animal.dto.AnimalResponse;
import app.vetra.animal.dto.CreateAnimalRequest;
import app.vetra.animal.service.AnimalService;
import app.vetra.auth.dto.FarmerRegisterRequest;
import app.vetra.auth.dto.VetRegisterRequest;
import app.vetra.auth.repository.UserRepository;
import app.vetra.auth.service.AuthService;
import app.vetra.disease.dto.DiseaseAnalyticsResponse;
import app.vetra.disease.dto.OperationalAlertResponse;
import app.vetra.disease.dto.OutbreakResponse;
import app.vetra.disease.dto.OutbreakStatisticsResponse;
import app.vetra.disease.entity.OutbreakStatus;
import app.vetra.disease.service.DiseaseAnalyticsService;
import app.vetra.disease.service.DiseaseService;
import app.vetra.disease.service.OperationalAlertService;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.infrastructure.persistence.enums.AnimalGender;
import app.vetra.infrastructure.persistence.enums.Species;
import app.vetra.infrastructure.persistence.enums.VerificationStatus;
import app.vetra.auth.repository.VetProfileRepository;
import app.vetra.mortality.dto.ConfirmMortalityRequest;
import app.vetra.mortality.dto.CreateMortalityReportRequest;
import app.vetra.mortality.dto.MortalityReportResponse;
import app.vetra.mortality.enums.MortalityCauseCategory;
import app.vetra.mortality.enums.MortalityReportStatus;
import app.vetra.mortality.enums.MortalitySource;
import app.vetra.mortality.service.MortalityReviewService;
import app.vetra.mortality.service.MortalityService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * End-to-End Acceptance Test validating Phase 3 mortality-aware epidemiological intelligence:
 * Farmer mortality report -> FARMER_REPORTED -> Vet confirmation -> VET_CONFIRMED ->
 * spatial/temporal/disease threshold evaluation -> Outbreak cluster creation/update ->
 * Government API aggregates -> Deterministic operational alert generation.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:vetra_phase3_e2e_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
      "vetra.ai.gateway.provider=noop",
      "vetra.ai.gemini.enabled=false",
      "vetra.firebase.enabled=false",
      "vetra.openmeteo.enabled=false"
    })
class Phase3EndToEndAcceptanceTest {

  @Autowired private AuthService authService;
  @Autowired private AnimalService animalService;
  @Autowired private UserRepository userRepository;
  @Autowired private VetProfileRepository vetProfileRepository;
  @Autowired private MortalityService mortalityService;
  @Autowired private MortalityReviewService mortalityReviewService;
  @Autowired private DiseaseService diseaseService;
  @Autowired private DiseaseAnalyticsService diseaseAnalyticsService;
  @Autowired private OperationalAlertService operationalAlertService;

  @Test
  @DisplayName("Full End-to-End Chain: Farmer report -> Vet confirmation -> Cluster intelligence -> Government APIs")
  void testPhase3CompleteEndToEndChain() {
    long timestamp = System.currentTimeMillis();

    // 1. Register Farmer in Baramati, Pune
    String farmerEmail = "farmer.baramati." + timestamp + "@vetra.app";
    authService.registerFarmer(
        new FarmerRegisterRequest(
            farmerEmail,
            "98765" + String.valueOf(timestamp).substring(8),
            "Password@123",
            "Balwant Kulkarni",
            "Kulkarni Dairy",
            "Baramati",
            "Baramati",
            "Pune",
            "Maharashtra",
            18.1511,
            74.5772,
            10,
            "mr"));

    // 2. Register Veterinarian in Baramati, Pune
    String vetEmail = "dr.shinde." + timestamp + "@vetra.app";
    authService.registerVet(
        new VetRegisterRequest(
            vetEmail,
            "98766" + String.valueOf(timestamp).substring(8),
            "Password@123",
            "Dr. Rajesh Shinde",
            "VET-REG-2026-991",
            "B.V.Sc & A.H.",
            "Epidemiology & Surgery",
            "Baramati Taluka Veterinary Hospital",
            10,
            18.1515,
            74.5775));

    User vetUser = userRepository.findByEmail(vetEmail).orElseThrow();
    VetProfile vetProfile = vetProfileRepository.findByUser(vetUser).orElseThrow();
    vetProfile.setVerificationStatus(VerificationStatus.VERIFIED);
    vetProfileRepository.save(vetProfile);

    // 3. Register Animal
    AnimalResponse animal = animalService.createAnimal(
        farmerEmail,
        new CreateAnimalRequest(
            "Sundari",
            "TAG-BARA-" + timestamp,
            "QR-BARA-" + timestamp,
            Species.CATTLE,
            "Gir",
            AnimalGender.FEMALE,
            LocalDate.now().minusYears(3),
            null));

    // 4. STEP 1: Farmer reports mortality
    CreateMortalityReportRequest reportRequest = new CreateMortalityReportRequest(
        animal.id(),
        MortalityCauseCategory.SUSPECTED_DISEASE,
        "Sudden death, bloody discharge from orifices",
        "Anthrax",
        false,
        null,
        "Carcass found in morning",
        Instant.now().minusSeconds(3600),
        18.1511,
        74.5772,
        5.0);

    MortalityReportResponse farmerReport = mortalityService.createMortalityReport(farmerEmail, reportRequest);
    assertNotNull(farmerReport);
    assertEquals(MortalityReportStatus.PENDING_REVIEW, farmerReport.status());
    assertEquals(MortalitySource.FARMER_REPORTED, farmerReport.source());

    // Verify: Single farmer mortality report does NOT create an outbreak cluster
    List<OutbreakResponse> initialOutbreaks = diseaseService.listOutbreaks(OutbreakStatus.ACTIVE);
    assertTrue(initialOutbreaks.isEmpty(), "Mortality alone must not create an outbreak cluster");

    // Verify: Government analytics reflects farmer report accurately
    DiseaseAnalyticsResponse initialAnalytics = diseaseAnalyticsService.getAnalytics();
    assertEquals(1L, initialAnalytics.totalMortalityReports());
    assertEquals(1L, initialAnalytics.farmerReportedMortalityCount());
    assertEquals(0L, initialAnalytics.vetConfirmedMortalityCount());

    // 5. STEP 2: Veterinarian validates and confirms mortality
    ConfirmMortalityRequest reviewRequest = new ConfirmMortalityRequest(
        MortalityCauseCategory.KNOWN_DISEASE,
        "Anthrax",
        "Bacillus anthracis spores identified in blood smear. Extreme bio-hazard.",
        true);

    MortalityReportResponse confirmedMortality =
        mortalityReviewService.confirmMortality(vetUser, farmerReport.id(), reviewRequest);
    assertEquals(MortalityReportStatus.CONFIRMED, confirmedMortality.status());
    assertEquals(MortalitySource.VET_CONFIRMED, confirmedMortality.source());

    // 6. STEP 3: Outbreak detection engine triggered by vet confirmation
    // Anthrax has minimumConfirmedCases = 1, so 1 vet-confirmed death satisfies the statutory disease threshold
    List<OutbreakResponse> activeOutbreaks = diseaseService.listOutbreaks(OutbreakStatus.ACTIVE);
    assertFalse(activeOutbreaks.isEmpty(), "Outbreak cluster must be detected for Anthrax after vet confirmation");

    OutbreakResponse outbreak = activeOutbreaks.get(0);
    assertEquals("Anthrax", outbreak.diseaseName());
    assertEquals(1, outbreak.mortalityCount());
    assertEquals(1, outbreak.vetConfirmedMortalityCount());
    assertEquals(0, outbreak.farmerReportedMortalityCount());
    assertTrue(outbreak.compositeRiskScore() >= 0 && outbreak.compositeRiskScore() <= 100);

    // 7. STEP 4: Government analytics returns real updated values
    DiseaseAnalyticsResponse finalAnalytics = diseaseAnalyticsService.getAnalytics();
    assertEquals(1L, finalAnalytics.totalMortalityReports());
    assertEquals(1L, finalAnalytics.vetConfirmedMortalityCount());

    OutbreakStatisticsResponse stats = diseaseService.getOutbreakStatistics();
    assertNotNull(stats.totalMortalities());
    assertEquals(1, stats.totalMortalities());
    assertEquals(1, stats.vetConfirmedMortalities());

    // 8. STEP 5: Operational alerts reflect cluster evidence
    List<OperationalAlertResponse> alerts = operationalAlertService.listOperationalAlerts();
    assertNotNull(alerts);
  }
}
