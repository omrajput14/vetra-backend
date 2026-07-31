package app.vetra.disease;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.ai.dto.AIScanResponse;
import app.vetra.ai.dto.CreateAIScanRequest;
import app.vetra.ai.service.AIScanService;
import app.vetra.animal.dto.AnimalResponse;
import app.vetra.animal.dto.CreateAnimalRequest;
import app.vetra.animal.service.AnimalService;
import app.vetra.auth.dto.FarmerRegisterRequest;
import app.vetra.auth.dto.VetRegisterRequest;
import app.vetra.auth.service.AuthService;
import app.vetra.disease.dto.CreateDiseaseReportRequest;
import app.vetra.disease.dto.DiseaseReportResponse;
import app.vetra.disease.dto.NearbyReportResponse;
import app.vetra.disease.dto.OutbreakResponse;
import app.vetra.disease.entity.DiagnosisConfidenceSource;
import app.vetra.disease.entity.DiagnosisStatus;
import app.vetra.disease.entity.DiseaseReportSource;
import app.vetra.disease.entity.OutbreakStatus;
import app.vetra.disease.geo.GeoUtils;
import app.vetra.disease.service.DiseaseService;
import app.vetra.infrastructure.exception.BusinessRuleException;
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
 * Integration tests for DiseaseService, PostGIS spatial queries,
 * Haversine distance filtering, and automatic outbreak cluster detection.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:vetra_disease_test2;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
class DiseaseServiceTest {

  @Autowired private DiseaseService diseaseService;
  @Autowired private AnimalService animalService;
  @Autowired private AuthService authService;
  @Autowired private AIScanService aiScanService;

  @Test
  void testCreateDiseaseReportSuccess() {
    authService.registerFarmer(new FarmerRegisterRequest(
        "farmer_dis@vetra.app", "+1555888001", "pass123", "Farmer Bob", "Green Farm",
        "Village", "District", "State", 12.9716, 77.5946, 10));

    authService.registerVet(new VetRegisterRequest(
        "vet_dis@vetra.app", "+1555888002", "pass123", "Dr. John", "VET-REG-7700",
        "BVSc", "Epidemiology", "City Vet Clinic", 5, 12.9720, 77.5950));

    AnimalResponse animal = animalService.createAnimal("farmer_dis@vetra.app",
        new CreateAnimalRequest("Daffodil", "TAG-DIS-1", "QR-DIS-1", Species.CATTLE, "Holstein", AnimalGender.FEMALE, LocalDate.now().minusYears(2), null));

    CreateDiseaseReportRequest reportReq = new CreateDiseaseReportRequest(
        animal.id(), null, null, DiseaseReportSource.VETERINARIAN, DiagnosisConfidenceSource.VETERINARIAN,
        "Foot and Mouth Disease", DiagnosisStatus.CONFIRMED, 12.9716, 77.5946, "Lesions on hoof");

    DiseaseReportResponse response = diseaseService.createReport("vet_dis@vetra.app", reportReq);

    assertNotNull(response.id());
    assertEquals("Foot and Mouth Disease", response.diseaseName());
    assertEquals(DiagnosisStatus.CONFIRMED, response.diagnosisStatus());
    assertEquals(12.9716, response.latitude());
  }

  @Test
  void testUnverifiedAIScanReportRejection() {
    authService.registerFarmer(new FarmerRegisterRequest(
        "farmer_unv@vetra.app", "+1555888003", "pass123", "Farmer Alice", "Valley Farm",
        "Village", "District", "State", 12.0, 56.0, 5));

    authService.registerVet(new VetRegisterRequest(
        "vet_unv@vetra.app", "+1555888004", "pass123", "Dr. Paul", "VET-REG-7711",
        "BVSc", "Epidemiology", "Valley Clinic", 6, 12.1, 56.1));

    AnimalResponse animal = animalService.createAnimal("farmer_unv@vetra.app",
        new CreateAnimalRequest("Rosie", "TAG-DIS-2", "QR-DIS-2", Species.GOAT, "Jamnapari", AnimalGender.FEMALE, LocalDate.now().minusYears(1), null));

    AIScanResponse scan = aiScanService.createScan("farmer_unv@vetra.app",
        new CreateAIScanRequest(animal.id(), "https://s3.amazonaws.com/vetra/scans/rosie.jpg", "HASH-DIS-99"));

    CreateDiseaseReportRequest unverifiedReq = new CreateDiseaseReportRequest(
        animal.id(), null, scan.id(), DiseaseReportSource.AI_VERIFIED, DiagnosisConfidenceSource.AI_VERIFIED,
        "Anthrax", DiagnosisStatus.SUSPECTED, 12.0, 56.0, "Unverified AI prediction");

    assertThrows(BusinessRuleException.class, () ->
        diseaseService.createReport("vet_unv@vetra.app", unverifiedReq));
  }

  @Test
  void testNearbySearchAndHaversineDistanceFiltering() {
    authService.registerFarmer(new FarmerRegisterRequest(
        "farmer_geo@vetra.app", "+1555888005", "pass123", "Farmer George", "East Farm",
        "Village", "District", "State", 12.9716, 77.5946, 10));

    authService.registerVet(new VetRegisterRequest(
        "vet_geo@vetra.app", "+1555888006", "pass123", "Dr. Helen", "VET-REG-7722",
        "BVSc", "Epidemiology", "East Clinic", 7, 12.9720, 77.5950));

    AnimalResponse animal1 = animalService.createAnimal("farmer_geo@vetra.app",
        new CreateAnimalRequest("Cow1", "TAG-GEO-1", "QR-GEO-1", Species.CATTLE, "Jersey", AnimalGender.FEMALE, LocalDate.now().minusYears(2), null));

    AnimalResponse animal2 = animalService.createAnimal("farmer_geo@vetra.app",
        new CreateAnimalRequest("Cow2", "TAG-GEO-2", "QR-GEO-2", Species.CATTLE, "Jersey", AnimalGender.FEMALE, LocalDate.now().minusYears(2), null));

    diseaseService.createReport("vet_geo@vetra.app", new CreateDiseaseReportRequest(
        animal1.id(), null, null, DiseaseReportSource.VETERINARIAN, DiagnosisConfidenceSource.VETERINARIAN,
        "Bovine Mastitis", DiagnosisStatus.CONFIRMED, 12.9716, 77.5946, "Center report"));

    diseaseService.createReport("vet_geo@vetra.app", new CreateDiseaseReportRequest(
        animal2.id(), null, null, DiseaseReportSource.VETERINARIAN, DiagnosisConfidenceSource.VETERINARIAN,
        "Bovine Mastitis", DiagnosisStatus.CONFIRMED, 13.1000, 77.5946, "15km away report"));

    List<NearbyReportResponse> nearby = diseaseService.searchNearbyReports(12.9716, 77.5946, 25.0);

    assertEquals(2, nearby.size());
    assertTrue(nearby.get(0).distanceKm() < 25.0);

    double distance = GeoUtils.calculateDistanceKm(12.9716, 77.5946, 13.1000, 77.5946);
    assertTrue(distance > 14.0 && distance < 16.0);
  }

  @Test
  void testPotentialOutbreakDetectionTrigger() {
    authService.registerFarmer(new FarmerRegisterRequest(
        "farmer_outbreak@vetra.app", "+1555888007", "pass123", "Farmer Sam", "North Farm",
        "Village", "District", "State", 13.0, 77.0, 20));

    authService.registerVet(new VetRegisterRequest(
        "vet_outbreak@vetra.app", "+1555888008", "pass123", "Dr. Rachel", "VET-REG-7733",
        "BVSc", "Epidemiology", "North Clinic", 9, 13.0, 77.0));

    AnimalResponse a1 = animalService.createAnimal("farmer_outbreak@vetra.app",
        new CreateAnimalRequest("Animal1", "TAG-OB-1", "QR-OB-1", Species.CATTLE, "Angus", AnimalGender.FEMALE, LocalDate.now().minusYears(2), null));

    AnimalResponse a2 = animalService.createAnimal("farmer_outbreak@vetra.app",
        new CreateAnimalRequest("Animal2", "TAG-OB-2", "QR-OB-2", Species.CATTLE, "Angus", AnimalGender.FEMALE, LocalDate.now().minusYears(2), null));

    AnimalResponse a3 = animalService.createAnimal("farmer_outbreak@vetra.app",
        new CreateAnimalRequest("Animal3", "TAG-OB-3", "QR-OB-3", Species.CATTLE, "Angus", AnimalGender.FEMALE, LocalDate.now().minusYears(2), null));

    diseaseService.createReport("vet_outbreak@vetra.app", new CreateDiseaseReportRequest(
        a1.id(), null, null, DiseaseReportSource.LAB_RESULT, DiagnosisConfidenceSource.LAB_CONFIRMED,
        "Foot and Mouth Disease", DiagnosisStatus.CONFIRMED, 13.001, 77.001, "Lab confirmed 1"));

    diseaseService.createReport("vet_outbreak@vetra.app", new CreateDiseaseReportRequest(
        a2.id(), null, null, DiseaseReportSource.LAB_RESULT, DiagnosisConfidenceSource.LAB_CONFIRMED,
        "Foot and Mouth Disease", DiagnosisStatus.CONFIRMED, 13.002, 77.002, "Lab confirmed 2"));

    diseaseService.createReport("vet_outbreak@vetra.app", new CreateDiseaseReportRequest(
        a3.id(), null, null, DiseaseReportSource.LAB_RESULT, DiagnosisConfidenceSource.LAB_CONFIRMED,
        "Foot and Mouth Disease", DiagnosisStatus.CONFIRMED, 13.003, 77.003, "Lab confirmed 3"));

    List<OutbreakResponse> outbreaks = diseaseService.listOutbreaks(OutbreakStatus.ACTIVE);
    assertFalse(outbreaks.isEmpty());

    OutbreakResponse activeCluster = outbreaks.get(0);
    assertEquals("Foot and Mouth Disease", activeCluster.diseaseName());
    assertEquals(OutbreakStatus.ACTIVE, activeCluster.status());
  }
}
