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
import app.vetra.disease.dto.DiseaseReportResponse;
import app.vetra.disease.dto.OutbreakResponse;
import app.vetra.disease.entity.DiagnosisConfidenceSource;
import app.vetra.disease.entity.DiagnosisStatus;
import app.vetra.disease.entity.DiseaseReportSource;
import app.vetra.disease.entity.OutbreakStatus;
import app.vetra.disease.risk.weather.WeatherData;
import app.vetra.disease.risk.weather.WeatherService;
import app.vetra.disease.service.DiseaseService;
import app.vetra.infrastructure.persistence.enums.AnimalGender;
import app.vetra.infrastructure.persistence.enums.Species;
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
 * End-to-end integration test verifying real Open-Meteo weather integration into
 * the VETRA surveillance pipeline:
 * GPS Coordinates -> WeatherService -> MultiSignalRiskEngine -> Outbreak -> OutbreakResponse.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:vetra_weather_e2e_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
      "vetra.ai.retry.backoff=1ms",
      "vetra.disease.risk-engine.weather-enabled=true",
      "vetra.disease.risk-engine.weather-api-base-url=https://api.open-meteo.com/v1/forecast"
    })
class WeatherEndToEndIntegrationTest {

  @Autowired private DiseaseService diseaseService;
  @Autowired private AnimalService animalService;
  @Autowired private AuthService authService;
  @Autowired private WeatherService weatherService;

  @Test
  @DisplayName("End-to-End: Outbreak in Pune, Maharashtra retrieves live Open-Meteo weather and populates risk breakdown")
  void testOutbreakWeatherIntegration() {
    // 1. Register farmer and vet
    authService.registerFarmer(
        new FarmerRegisterRequest(
            "pune_farmer@vetra.app",
            "+919876543201",
            "securePass123",
            "Pune Farmer",
            "Sahyadri Dairy Farm",
            "Haveli",
            "Pune",
            "Maharashtra",
            18.5204,
            73.8567,
            25));

    authService.registerVet(
        new VetRegisterRequest(
            "pune_vet@vetra.app",
            "+919876543202",
            "securePass123",
            "Dr. Ramesh Deshmukh",
            "VET-MH-2024-8891",
            "BVSc",
            "Epidemiology",
            "Govt Veterinary Hospital Haveli",
            12,
            18.5204,
            73.8567));

    // 2. Register animals
    AnimalResponse animal1 =
        animalService.createAnimal(
            "pune_farmer@vetra.app",
            new CreateAnimalRequest(
                "Gauri",
                "TAG-MH-PUN-001",
                "QR-MH-PUN-001",
                Species.CATTLE,
                "Gir",
                AnimalGender.FEMALE,
                LocalDate.now().minusYears(3),
                null));

    AnimalResponse animal2 =
        animalService.createAnimal(
            "pune_farmer@vetra.app",
            new CreateAnimalRequest(
                "Lakshmi",
                "TAG-MH-PUN-002",
                "QR-MH-PUN-002",
                Species.CATTLE,
                "Sahiwal",
                AnimalGender.FEMALE,
                LocalDate.now().minusYears(4),
                null));

    AnimalResponse animal3 =
        animalService.createAnimal(
            "pune_farmer@vetra.app",
            new CreateAnimalRequest(
                "Nandi",
                "TAG-MH-PUN-003",
                "QR-MH-PUN-003",
                Species.BUFFALO,
                "Murrah",
                AnimalGender.MALE,
                LocalDate.now().minusYears(2),
                null));

    // 3. Create 3 confirmed FMD reports at Pune coordinates (18.5204, 73.8567) to meet cluster threshold
    diseaseService.createReport(
        "pune_vet@vetra.app",
        new CreateDiseaseReportRequest(
            animal1.id(),
            null,
            null,
            DiseaseReportSource.VETERINARIAN,
            DiagnosisConfidenceSource.VETERINARIAN,
            "Foot and Mouth Disease",
            DiagnosisStatus.CONFIRMED,
            18.5204,
            73.8567,
            "Acute vesicles observed in oral cavity and blisters on tongue"));

    diseaseService.createReport(
        "pune_vet@vetra.app",
        new CreateDiseaseReportRequest(
            animal2.id(),
            null,
            null,
            DiseaseReportSource.VETERINARIAN,
            DiagnosisConfidenceSource.VETERINARIAN,
            "Foot and Mouth Disease",
            DiagnosisStatus.CONFIRMED,
            18.5210,
            73.8570,
            "Secondary confirmed case in same sector with fever"));

    diseaseService.createReport(
        "pune_vet@vetra.app",
        new CreateDiseaseReportRequest(
            animal3.id(),
            null,
            null,
            DiseaseReportSource.VETERINARIAN,
            DiagnosisConfidenceSource.VETERINARIAN,
            "Foot and Mouth Disease",
            DiagnosisStatus.CONFIRMED,
            18.5220,
            73.8580,
            "Tertiary confirmed case in same herd with excessive salivation"));

    // 4. Retrieve generated outbreak cluster
    List<OutbreakResponse> outbreaks = diseaseService.listOutbreaks(OutbreakStatus.ACTIVE);
    assertNotNull(outbreaks);
    assertFalse(outbreaks.isEmpty(), "An active outbreak cluster should be created");

    OutbreakResponse outbreak =
        outbreaks.stream()
            .filter(o -> o.diseaseName().equals("Foot and Mouth Disease"))
            .findFirst()
            .orElseThrow();

    // 5. Verify Weather context is present and populated from live Open-Meteo
    assertNotNull(outbreak.riskBreakdown(), "Risk breakdown should be present");
    assertNotNull(outbreak.riskBreakdown().weatherScore(), "Weather score should be computed");
    assertTrue(
        outbreak.riskBreakdown().weatherScore() >= 0.0 && outbreak.riskBreakdown().weatherScore() <= 100.0,
        "Weather score must be normalized 0-100");

    assertNotNull(outbreak.riskBreakdown().weatherTemperature(), "Live temperature should be retrieved");
    assertNotNull(outbreak.riskBreakdown().weatherHumidity(), "Live humidity should be retrieved");
    assertNotNull(outbreak.riskBreakdown().weatherPrecipitation(), "Live precipitation should be retrieved");

    // Realistic physical boundaries for Pune, Maharashtra
    assertTrue(
        outbreak.riskBreakdown().weatherTemperature() > 0.0
            && outbreak.riskBreakdown().weatherTemperature() < 55.0,
        "Temperature should be within realistic ambient range");
    assertTrue(
        outbreak.riskBreakdown().weatherHumidity() >= 10.0
            && outbreak.riskBreakdown().weatherHumidity() <= 100.0,
        "Relative humidity should be between 10% and 100%");

    // 6. Verify explanation includes meteorological status
    assertNotNull(outbreak.riskBreakdown().riskExplanation());
    assertTrue(
        outbreak.riskBreakdown().riskExplanation().contains("Weather:"),
        "Explanation should reference weather signal");

    // 7. Verify composite risk score incorporates all signals
    assertNotNull(outbreak.compositeRiskScore());
    assertTrue(outbreak.compositeRiskScore() > 0);
  }
}
