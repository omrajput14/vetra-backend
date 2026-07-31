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
import app.vetra.disease.dto.CreateDiseaseReportRequest;
import app.vetra.disease.dto.DiseaseAnalyticsResponse;
import app.vetra.disease.dto.OutbreakResponse;
import app.vetra.disease.engine.OutbreakScheduler;
import app.vetra.disease.entity.DiagnosisConfidenceSource;
import app.vetra.disease.entity.DiagnosisStatus;
import app.vetra.disease.entity.DiseaseReportSource;
import app.vetra.disease.entity.OutbreakStatus;
import app.vetra.disease.geo.GeoJsonFeatureCollection;
import app.vetra.disease.geo.GeoJsonService;
import app.vetra.disease.geo.HeatmapPoint;
import app.vetra.disease.registry.DiseaseMetadata;
import app.vetra.disease.registry.DiseaseRegistryService;
import app.vetra.disease.service.DiseaseAnalyticsService;
import app.vetra.disease.service.DiseaseService;
import app.vetra.infrastructure.persistence.enums.AnimalGender;
import app.vetra.infrastructure.persistence.enums.Species;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test suite for Autonomous Disease Intelligence Engine (Stage 10.3),
 * GeoJSON export, spatial heatmaps, scheduled re-evaluation, and disease registry.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:vetra_auto_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
class AutonomousDiseaseEngineTest {

  @Autowired private DiseaseService diseaseService;
  @Autowired private AnimalService animalService;
  @Autowired private AuthService authService;
  @Autowired private DiseaseRegistryService registryService;
  @Autowired private GeoJsonService geoJsonService;
  @Autowired private DiseaseAnalyticsService analyticsService;
  @Autowired private OutbreakScheduler outbreakScheduler;

  @Test
  void testDiseaseRegistryCatalog() {
    List<DiseaseMetadata> all = registryService.getAllDiseases();
    assertFalse(all.isEmpty());

    DiseaseMetadata rabies = registryService.getDiseaseByName("Rabies").orElse(null);
    assertNotNull(rabies);
    assertTrue(rabies.zoonotic());
    assertTrue(rabies.reportable());
    assertEquals(50.0, rabies.defaultRadiusKm());
  }

  @Test
  void testGeoJsonExportAndHeatmapData() {
    authService.registerFarmer(new FarmerRegisterRequest(
        "farmer_json@vetra.app", "+1555777001", "pass123", "Farmer Json", "Farm",
        "Village", "District", "State", 12.0, 77.0, 10));

    authService.registerVet(new VetRegisterRequest(
        "vet_json@vetra.app", "+1555777002", "pass123", "Dr. Json", "VET-JSON-1",
        "BVSc", "Epidemiology", "Clinic", 5, 12.0, 77.0));

    AnimalResponse animal = animalService.createAnimal("farmer_json@vetra.app",
        new CreateAnimalRequest("DogJson", "TAG-JSON-1", "QR-JSON-1", Species.OTHER, "Indie", AnimalGender.MALE, LocalDate.now().minusYears(3), null));

    diseaseService.createReport("vet_json@vetra.app", new CreateDiseaseReportRequest(
        animal.id(), null, null, DiseaseReportSource.LAB_RESULT, DiagnosisConfidenceSource.LAB_CONFIRMED,
        "Rabies", DiagnosisStatus.CONFIRMED, 12.0, 77.0, "Lab confirmed rabies"));

    GeoJsonFeatureCollection geoJson = geoJsonService.getOutbreaksGeoJson();
    assertNotNull(geoJson);
    assertEquals("FeatureCollection", geoJson.type());
    assertFalse(geoJson.features().isEmpty());

    List<HeatmapPoint> heatmap = geoJsonService.getHeatmapData();
    assertFalse(heatmap.isEmpty());
    assertTrue(heatmap.get(0).intensityWeight() > 0.0);
  }

  @Test
  void testDiseaseAnalyticsMetrics() {
    DiseaseAnalyticsResponse analytics = analyticsService.getAnalytics();
    assertNotNull(analytics);
    assertTrue(analytics.diseaseDistribution() != null);
  }
}
