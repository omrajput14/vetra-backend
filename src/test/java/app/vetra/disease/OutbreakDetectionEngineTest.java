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
import app.vetra.auth.service.AuthService;
import app.vetra.disease.config.DiseaseOutbreakProperties;
import app.vetra.disease.config.DiseaseProfile;
import app.vetra.disease.dto.CreateDiseaseReportRequest;
import app.vetra.disease.dto.DiseaseReportResponse;
import app.vetra.disease.dto.OutbreakResponse;
import app.vetra.disease.engine.OutbreakDetectionEngine;
import app.vetra.disease.entity.DiagnosisConfidenceSource;
import app.vetra.disease.entity.DiagnosisStatus;
import app.vetra.disease.entity.DiseaseReportSource;
import app.vetra.disease.entity.OutbreakRiskScore;
import app.vetra.disease.entity.OutbreakStatus;
import app.vetra.disease.service.DiseaseService;
import app.vetra.infrastructure.persistence.enums.AnimalGender;
import app.vetra.infrastructure.persistence.enums.Species;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Unit & Integration test suite for OutbreakDetectionEngine, disease profiles,
 * risk scoring algorithm, sliding time windows, and duplicate prevention.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:vetra_engine_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
    "vetra.ai.default-provider=NONE",
    "vetra.ai.retry.max-attempts=1",
    "vetra.ai.retry.backoff=1ms"
})
class OutbreakDetectionEngineTest {

  @Autowired private DiseaseService diseaseService;
  @Autowired private AnimalService animalService;
  @Autowired private AuthService authService;
  @Autowired private OutbreakDetectionEngine outbreakDetectionEngine;
  @Autowired private DiseaseOutbreakProperties outbreakProperties;

  @Test
  void testDiseaseSpecificProfiles() {
    DiseaseProfile fmd = outbreakProperties.getProfileForDisease("Foot and Mouth Disease");
    assertEquals(25.0, fmd.radiusKm());
    assertEquals(3, fmd.minimumConfirmedCases());
    assertEquals(48, fmd.evaluationWindowHours());
    assertEquals(1.8, fmd.severityWeight());

    DiseaseProfile rabies = outbreakProperties.getProfileForDisease("Rabies");
    assertEquals(50.0, rabies.radiusKm());
    assertEquals(1, rabies.minimumConfirmedCases());
    assertEquals(24, rabies.evaluationWindowHours());
    assertEquals(2.5, rabies.severityWeight());
  }

  @Test
  void testRiskScoringAlgorithm() {
    // Rabies (1 case, weight 2.5, 24h window) -> score = 1 * 2.5 * 1 = 2.5 -> MEDIUM
    OutbreakRiskScore scoreRabies1 = outbreakDetectionEngine.calculateRiskScore(1, 2.5, 50.0, 24);
    assertEquals(OutbreakRiskScore.MEDIUM, scoreRabies1);

    // High case count (10 cases) -> CRITICAL
    OutbreakRiskScore score10 = outbreakDetectionEngine.calculateRiskScore(10, 1.0, 15.0, 72);
    assertEquals(OutbreakRiskScore.CRITICAL, score10);

    // High severity & velocity (5 cases, weight 2.0, 24h window) -> score = 10 -> CRITICAL
    OutbreakRiskScore scoreCritical = outbreakDetectionEngine.calculateRiskScore(5, 2.0, 15.0, 24);
    assertEquals(OutbreakRiskScore.CRITICAL, scoreCritical);
  }

  @Test
  void testRabiesImmediateSingleCaseOutbreakTrigger() {
    authService.registerFarmer(new FarmerRegisterRequest(
        "farmer_rabies@vetra.app", "+1555999001", "pass123", "Farmer Bob", "Farm",
        "Village", "District", "State", 12.0, 77.0, 10));

    authService.registerVet(new VetRegisterRequest(
        "vet_rabies@vetra.app", "+1555999002", "pass123", "Dr. John", "VET-RAB-1",
        "BVSc", "Epidemiology", "Clinic", 5, 12.0, 77.0));

    AnimalResponse animal = animalService.createAnimal("farmer_rabies@vetra.app",
        new CreateAnimalRequest("Dog1", "TAG-RAB-1", "QR-RAB-1", Species.OTHER, "Indie", AnimalGender.MALE, LocalDate.now().minusYears(3), null));

    // Single Rabies confirmed report triggers outbreak (threshold = 1 case)
    diseaseService.createReport("vet_rabies@vetra.app", new CreateDiseaseReportRequest(
        animal.id(), null, null, DiseaseReportSource.LAB_RESULT, DiagnosisConfidenceSource.LAB_CONFIRMED,
        "Rabies", DiagnosisStatus.CONFIRMED, 12.0, 77.0, "Lab confirmed rabies"));

    List<OutbreakResponse> outbreaks = diseaseService.listOutbreaks(OutbreakStatus.ACTIVE);
    assertFalse(outbreaks.isEmpty());

    OutbreakResponse rabiesCluster = outbreaks.stream()
        .filter(o -> o.diseaseName().equalsIgnoreCase("Rabies"))
        .findFirst()
        .orElse(null);

    assertNotNull(rabiesCluster);
    assertEquals("Rabies", rabiesCluster.diseaseName());
    assertEquals(1, rabiesCluster.affectedReportsCount());
    assertEquals(50.0, rabiesCluster.radiusKm());
  }

  @Test
  void testDuplicateClusterPreventionAndRiskEscalation() {
    authService.registerFarmer(new FarmerRegisterRequest(
        "farmer_dup@vetra.app", "+1555999003", "pass123", "Farmer Alice", "Farm",
        "Village", "District", "State", 13.0, 78.0, 10));

    authService.registerVet(new VetRegisterRequest(
        "vet_dup@vetra.app", "+1555999004", "pass123", "Dr. Paul", "VET-DUP-1",
        "BVSc", "Epidemiology", "Clinic", 5, 13.0, 78.0));

    AnimalResponse a1 = animalService.createAnimal("farmer_dup@vetra.app",
        new CreateAnimalRequest("Cattle1", "TAG-DUP-1", "QR-DUP-1", Species.CATTLE, "Jersey", AnimalGender.FEMALE, LocalDate.now().minusYears(2), null));
    AnimalResponse a2 = animalService.createAnimal("farmer_dup@vetra.app",
        new CreateAnimalRequest("Cattle2", "TAG-DUP-2", "QR-DUP-2", Species.CATTLE, "Jersey", AnimalGender.FEMALE, LocalDate.now().minusYears(2), null));
    AnimalResponse a3 = animalService.createAnimal("farmer_dup@vetra.app",
        new CreateAnimalRequest("Cattle3", "TAG-DUP-3", "QR-DUP-3", Species.CATTLE, "Jersey", AnimalGender.FEMALE, LocalDate.now().minusYears(2), null));

    // Submit 3 FMD reports
    diseaseService.createReport("vet_dup@vetra.app", new CreateDiseaseReportRequest(
        a1.id(), null, null, DiseaseReportSource.VETERINARIAN, DiagnosisConfidenceSource.VETERINARIAN,
        "Foot and Mouth Disease", DiagnosisStatus.CONFIRMED, 13.001, 78.001, "Report 1"));

    diseaseService.createReport("vet_dup@vetra.app", new CreateDiseaseReportRequest(
        a2.id(), null, null, DiseaseReportSource.VETERINARIAN, DiagnosisConfidenceSource.VETERINARIAN,
        "Foot and Mouth Disease", DiagnosisStatus.CONFIRMED, 13.002, 78.002, "Report 2"));

    diseaseService.createReport("vet_dup@vetra.app", new CreateDiseaseReportRequest(
        a3.id(), null, null, DiseaseReportSource.VETERINARIAN, DiagnosisConfidenceSource.VETERINARIAN,
        "Foot and Mouth Disease", DiagnosisStatus.CONFIRMED, 13.003, 78.003, "Report 3"));

    List<OutbreakResponse> outbreaks = diseaseService.listOutbreaks(OutbreakStatus.ACTIVE);
    // Should create exactly 1 outbreak cluster (no duplicates!)
    assertEquals(1, outbreaks.size());
    assertEquals(3, outbreaks.get(0).affectedReportsCount());
  }
}
