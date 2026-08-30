package app.vetra.disease;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.ai.dto.AIScanResponse;
import app.vetra.ai.dto.ApproveAIScanRequest;
import app.vetra.ai.dto.CreateAIScanRequest;
import app.vetra.ai.service.AIScanService;
import app.vetra.animal.dto.AnimalResponse;
import app.vetra.animal.dto.CreateAnimalRequest;
import app.vetra.animal.service.AnimalService;
import app.vetra.auth.dto.FarmerRegisterRequest;
import app.vetra.auth.dto.VetRegisterRequest;
import app.vetra.auth.service.AuthService;
import app.vetra.disease.controller.OutbreakController;
import app.vetra.disease.dto.AIScreeningResponse;
import app.vetra.disease.dto.CreateDiseaseReportRequest;
import app.vetra.disease.dto.OutbreakResponse;
import app.vetra.disease.entity.DiagnosisConfidenceSource;
import app.vetra.disease.entity.DiagnosisStatus;
import app.vetra.disease.entity.DiseaseReportSource;
import app.vetra.disease.entity.OutbreakStatus;
import app.vetra.disease.service.DiseaseService;
import app.vetra.infrastructure.persistence.enums.AnimalGender;
import app.vetra.infrastructure.persistence.enums.Species;
import app.vetra.infrastructure.response.ApiResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test validating the complete surveillance workflow:
 * AI Preliminary Screening -> Government Dashboard Visibility -> Veterinarian Verification -> Outbreak Detection.
 *
 * <p>Strictly validates the domain invariants:
 * 1. AI Scan is preliminary screening and is visible on /api/v1/disease/ai-screenings with veterinarianVerified=false.
 * 2. An AI scan alone NEVER declares an outbreak.
 * 3. Only clinical / lab confirmed disease reports trigger outbreak clustering.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:vetra_ai_screening_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
class AIScreeningIntegrationTest {

  @Autowired private AuthService authService;
  @Autowired private AnimalService animalService;
  @Autowired private AIScanService aiScanService;
  @Autowired private DiseaseService diseaseService;
  @Autowired private OutbreakController outbreakController;

  @Test
  @DisplayName("Full Pipeline: Farmer AI Scan -> Government Surveillance Visibility -> Vet Review -> Outbreak Detection")
  void testEndToEndAIScreeningToOutbreakFlow() {
    // 1. Register Farmer in Pune with GPS coordinates
    String farmerEmail = "farmer.pune." + System.currentTimeMillis() + "@vetra.app";
    authService.registerFarmer(
        new FarmerRegisterRequest(
            farmerEmail,
            "9898980001",
            "Password@123",
            "Ramesh Patil",
            "Patil Dairy Farm",
            "Wagholi",
            "Haveli",
            "Pune",
            "Maharashtra",
            18.5204,
            73.8567,
            12,
            "en"));

    // 2. Register Veterinarian
    String vetEmail = "dr.deshmukh." + System.currentTimeMillis() + "@vetra.app";
    authService.registerVet(
        new VetRegisterRequest(
            vetEmail,
            "9898980002",
            "Password@123",
            "Dr. Sanjay Deshmukh",
            "VET-REG-2026-001",
            "BVSc",
            "Large Animal",
            "Pune Clinic",
            10,
            18.5204,
            73.8567));

    // 3. Register Animal
    AnimalResponse animal =
        animalService.createAnimal(
            farmerEmail,
            new CreateAnimalRequest(
                "Gauri",
                "TAG-PUNE-001",
                "QR-PUNE-001",
                Species.CATTLE,
                "Gir",
                AnimalGender.FEMALE,
                LocalDate.now().minusYears(3),
                null));
    assertNotNull(animal);

    // 4. Farmer uploads AI diagnostic scan
    AIScanResponse scan =
        aiScanService.createScan(
            farmerEmail,
            new CreateAIScanRequest(
                animal.id(),
                "https://s3.aws.amazon.com/vetra-scans/skin_lesion_01.jpg",
                "Farmer noticed skin nodules and high fever"));
    assertNotNull(scan);
    assertFalse(scan.veterinarianVerified(), "AI Scan must initially be unverified by veterinarian");

    // 5. Query Government AI Screening surveillance endpoint
    ApiResponse<List<AIScreeningResponse>> govScreenings =
        outbreakController.listAIScreenings(null);
    assertNotNull(govScreenings);
    assertNotNull(govScreenings.data());
    assertFalse(govScreenings.data().isEmpty());

    AIScreeningResponse screening =
        govScreenings.data().stream()
            .filter(s -> s.id().equals(scan.id()))
            .findFirst()
            .orElseThrow();

    // Verify surveillance payload correctness
    assertEquals(animal.id(), screening.animalId());
    assertEquals("TAG-PUNE-001", screening.tagNumber());
    assertEquals("Gauri", screening.animalName());
    assertEquals("CATTLE", screening.species());
    assertFalse(screening.veterinarianVerified(), "Status must be awaiting veterinary verification");
    assertEquals("AI_PRELIMINARY_SCREENING", screening.source());
    assertEquals(18.5204, screening.latitude());
    assertEquals(73.8567, screening.longitude());
    assertEquals("Pune", screening.district());
    assertEquals("Haveli", screening.taluka());
    assertEquals("Maharashtra", screening.state());
    assertNotNull(screening.preliminaryDiagnosis());

    // 6. Verify that a single (or multiple) AI scan(s) DO NOT declare an outbreak
    List<OutbreakResponse> outbreaksBeforeClinicalVerification =
        diseaseService.listOutbreaks(OutbreakStatus.ACTIVE);
    assertTrue(
        outbreaksBeforeClinicalVerification.isEmpty(),
        "INVARIANT: AI screening scans alone must NEVER declare an official outbreak");

    // 7. Query with filter veterinarianVerified = false vs true
    ApiResponse<List<AIScreeningResponse>> unverifiedList =
        outbreakController.listAIScreenings(false);
    assertTrue(unverifiedList.data().stream().anyMatch(s -> s.id().equals(scan.id())));

    ApiResponse<List<AIScreeningResponse>> verifiedList =
        outbreakController.listAIScreenings(true);
    assertFalse(verifiedList.data().stream().anyMatch(s -> s.id().equals(scan.id())));

    // 8. Paginated query test
    ApiResponse<Page<AIScreeningResponse>> paginated =
        outbreakController.listAIScreeningsPaginated(false, PageRequest.of(0, 10));
    assertNotNull(paginated.data());
    assertTrue(paginated.data().getContent().stream().anyMatch(s -> s.id().equals(scan.id())));

    // 9. Single ID query test
    ApiResponse<AIScreeningResponse> singleScreening =
        outbreakController.getAIScreeningById(scan.id());
    assertNotNull(singleScreening.data());
    assertEquals(scan.id(), singleScreening.data().id());

    // 10. Licensed Veterinarian verifies and approves the AI scan
    AIScanResponse approvedScan =
        aiScanService.approveScan(
            vetEmail,
            scan.id(),
            new ApproveAIScanRequest(
                "Clinical examination confirms nodules and fever characteristic of LSD.",
                "Lumpy Skin Disease",
                "Isolate animal and administer symptomatic anti-inflammatory treatment."));
    assertTrue(approvedScan.veterinarianVerified());

    // 11. Now verifiedList includes this scan
    ApiResponse<List<AIScreeningResponse>> updatedVerifiedList =
        outbreakController.listAIScreenings(true);
    assertTrue(updatedVerifiedList.data().stream().anyMatch(s -> s.id().equals(scan.id())));

    // 12. Submit clinical disease reports by veterinarian to trigger official outbreak clustering
    AnimalResponse animal2 =
        animalService.createAnimal(
            farmerEmail,
            new CreateAnimalRequest(
                "Radha",
                "TAG-PUNE-002",
                "QR-PUNE-002",
                Species.CATTLE,
                "Gir",
                AnimalGender.FEMALE,
                LocalDate.now().minusYears(2),
                null));

    AnimalResponse animal3 =
        animalService.createAnimal(
            farmerEmail,
            new CreateAnimalRequest(
                "Shanti",
                "TAG-PUNE-003",
                "QR-PUNE-003",
                Species.CATTLE,
                "Sahiwal",
                AnimalGender.FEMALE,
                LocalDate.now().minusYears(4),
                null));

    // Vet creates confirmed clinical reports linked to verified scan & animals in Pune
    diseaseService.createReport(
        vetEmail,
        new CreateDiseaseReportRequest(
            animal.id(),
            null,
            scan.id(),
            DiseaseReportSource.AI_VERIFIED,
            DiagnosisConfidenceSource.AI_VERIFIED,
            "Lumpy Skin Disease",
            DiagnosisStatus.CONFIRMED,
            18.5204,
            73.8567,
            "Clinically verified LSD"));

    diseaseService.createReport(
        vetEmail,
        new CreateDiseaseReportRequest(
            animal2.id(),
            null,
            null,
            DiseaseReportSource.VETERINARIAN,
            DiagnosisConfidenceSource.VETERINARIAN,
            "Lumpy Skin Disease",
            DiagnosisStatus.CONFIRMED,
            18.5220,
            73.8580,
            "Secondary clinical case in vicinity"));

    diseaseService.createReport(
        vetEmail,
        new CreateDiseaseReportRequest(
            animal3.id(),
            null,
            null,
            DiseaseReportSource.VETERINARIAN,
            DiagnosisConfidenceSource.VETERINARIAN,
            "Lumpy Skin Disease",
            DiagnosisStatus.CONFIRMED,
            18.5230,
            73.8590,
            "Tertiary clinical case in vicinity"));

    // 13. Now and only now, after 3 confirmed clinical reports, an outbreak cluster is detected
    List<OutbreakResponse> activeOutbreaks = diseaseService.listOutbreaks(OutbreakStatus.ACTIVE);
    assertFalse(activeOutbreaks.isEmpty(), "Outbreak cluster must be detected from 3 confirmed clinical reports");
    OutbreakResponse outbreak = activeOutbreaks.get(0);
    assertEquals("Lumpy Skin Disease", outbreak.diseaseName());
    assertEquals(3, outbreak.affectedReportsCount());
  }
}
