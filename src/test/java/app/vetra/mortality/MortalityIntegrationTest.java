package app.vetra.mortality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.vetra.animal.dto.AnimalResponse;
import app.vetra.animal.dto.CreateAnimalRequest;
import app.vetra.animal.repository.AnimalHealthRecordRepository;
import app.vetra.animal.repository.AnimalRepository;
import app.vetra.animal.service.AnimalService;
import app.vetra.auth.dto.AuthResponse;
import app.vetra.auth.dto.FarmerRegisterRequest;
import app.vetra.auth.dto.LoginRequest;
import app.vetra.auth.dto.VetRegisterRequest;
import app.vetra.auth.service.AuthService;
import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.enums.AnimalGender;
import app.vetra.infrastructure.persistence.enums.AnimalStatus;
import app.vetra.infrastructure.persistence.enums.HealthRecordType;
import app.vetra.infrastructure.persistence.enums.Species;
import app.vetra.mortality.entity.AnimalMortalityEvent;
import app.vetra.mortality.enums.MortalityCauseCategory;
import app.vetra.mortality.enums.MortalityReportStatus;
import app.vetra.mortality.enums.MortalitySource;
import app.vetra.mortality.repository.AnimalMortalityEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for Phase 1: Mortality Data Foundation and Farmer Mortality Reporting.
 *
 * <p>Validates strict farmer authorization, animal ownership checks, disease registry lookup,
 * idempotency retries, duplicate prevention, and health timeline logging.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:vetra_mortality_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
class MortalityIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AuthService authService;
  @Autowired private AnimalService animalService;
  @Autowired private AnimalRepository animalRepository;
  @Autowired private AnimalMortalityEventRepository mortalityRepository;
  @Autowired private AnimalHealthRecordRepository healthRecordRepository;
  @Autowired private ObjectMapper objectMapper;

  private String farmerTokenA;
  private String farmerEmailA;
  private String farmerTokenB;
  private String farmerEmailB;
  private String vetToken;
  private AnimalResponse testAnimalA;

  @BeforeEach
  void setUp() {
    long timestamp = System.currentTimeMillis();

    // Register Farmer A
    farmerEmailA = "farmer.a." + timestamp + "@vetra.app";
    authService.registerFarmer(
        new FarmerRegisterRequest(
            farmerEmailA,
            "98980" + (timestamp % 100000),
            "Password@123",
            "Ramesh Patil",
            "Patil Farm",
            "Haveli",
            "Pune",
            "Pune",
            "Maharashtra",
            18.5204,
            73.8567,
            5,
            "en"));
    AuthResponse loginA = authService.loginFarmer(new LoginRequest(farmerEmailA, "Password@123"));
    farmerTokenA = loginA.accessToken();

    // Register Farmer B
    farmerEmailB = "farmer.b." + timestamp + "@vetra.app";
    authService.registerFarmer(
        new FarmerRegisterRequest(
            farmerEmailB,
            "98981" + (timestamp % 100000),
            "Password@123",
            "Suresh Deshmukh",
            "Deshmukh Dairy",
            "Baramati",
            "Pune",
            "Pune",
            "Maharashtra",
            18.1500,
            74.5800,
            10,
            "en"));
    AuthResponse loginB = authService.loginFarmer(new LoginRequest(farmerEmailB, "Password@123"));
    farmerTokenB = loginB.accessToken();

    // Register Vet
    String vetEmail = "vet." + timestamp + "@vetra.app";
    authService.registerVet(
        new VetRegisterRequest(
            vetEmail,
            "98982" + (timestamp % 100000),
            "Password@123",
            "Dr. Anand Kulkarni",
            "VET-REG-" + timestamp,
            "BVSc",
            "Large Animal",
            "Pune Vet Clinic",
            8,
            18.5204,
            73.8567));
    AuthResponse vetLogin = authService.loginVet(new LoginRequest(vetEmail, "Password@123"));
    vetToken = vetLogin.accessToken();

    // Register Animal owned by Farmer A
    testAnimalA =
        animalService.createAnimal(
            farmerEmailA,
            new CreateAnimalRequest(
                "Nandi",
                "TAG-A-" + timestamp,
                "QR-A-" + timestamp,
                Species.CATTLE,
                "Gir",
                AnimalGender.MALE,
                null,
                null));
  }

  @Test
  @DisplayName("1. Successfully reports animal mortality with known disease and GPS")
  void testCreateMortalityReport_Success() throws Exception {
    String payload =
        """
        {
          "animalId": "%s",
          "causeCategory": "KNOWN_DISEASE",
          "diseaseName": "Foot and Mouth Disease",
          "causeDescription": "High fever followed by blisters on feet and mouth",
          "recentlyTreated": true,
          "treatmentNotes": "Administered antibiotics 2 days ago",
          "notes": "Isolated from other cattle immediately",
          "latitude": 18.5204,
          "longitude": 73.8567,
          "locationAccuracy": 4.5
        }
        """
            .formatted(testAnimalA.id());

    mockMvc
        .perform(
            post("/api/v1/mortalities")
                .header("Authorization", "Bearer " + farmerTokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.animalId").value(testAnimalA.id().toString()))
        .andExpect(jsonPath("$.data.tagNumber").value(testAnimalA.tagNumber()))
        .andExpect(jsonPath("$.data.causeCategory").value("KNOWN_DISEASE"))
        .andExpect(jsonPath("$.data.diseaseName").value("Foot and Mouth Disease"))
        .andExpect(jsonPath("$.data.source").value("FARMER_REPORTED"))
        .andExpect(jsonPath("$.data.status").value("REPORTED"));

    // Verify animal state changed to DECEASED
    Animal animal = animalRepository.findById(testAnimalA.id()).orElseThrow();
    assertThat(animal.getStatus()).isEqualTo(AnimalStatus.DECEASED);

    // Verify health timeline record recorded
    assertThat(healthRecordRepository.findByAnimalIdOrderByRecordedAtDesc(testAnimalA.id()))
        .anyMatch(r -> r.getRecordType() == HealthRecordType.MORTALITY);
  }

  @Test
  @DisplayName("2. Successfully reports animal mortality with UNKNOWN cause ('I don't know')")
  void testCreateMortalityReport_UnknownCause() throws Exception {
    String payload =
        """
        {
          "animalId": "%s",
          "causeCategory": "UNKNOWN",
          "notes": "Found deceased in the morning, cause unknown"
        }
        """
            .formatted(testAnimalA.id());

    mockMvc
        .perform(
            post("/api/v1/mortalities")
                .header("Authorization", "Bearer " + farmerTokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.causeCategory").value("UNKNOWN"));

    Animal animal = animalRepository.findById(testAnimalA.id()).orElseThrow();
    assertThat(animal.getStatus()).isEqualTo(AnimalStatus.DECEASED);
  }

  @Test
  @DisplayName("3. Fails with 400 when required fields are missing")
  void testCreateMortalityReport_MissingRequiredFields() throws Exception {
    // Missing animalId and causeCategory
    String payload = """
        {
          "notes": "Incomplete report"
        }
        """;

    mockMvc
        .perform(
            post("/api/v1/mortalities")
                .header("Authorization", "Bearer " + farmerTokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false));
  }

  @Test
  @DisplayName("4. Fails with 404 when animal ID does not exist")
  void testCreateMortalityReport_InvalidAnimalId() throws Exception {
    UUID randomId = UUID.randomUUID();
    String payload =
        """
        {
          "animalId": "%s",
          "causeCategory": "ACCIDENT_INJURY"
        }
        """
            .formatted(randomId);

    mockMvc
        .perform(
            post("/api/v1/mortalities")
                .header("Authorization", "Bearer " + farmerTokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false));
  }

  @Test
  @DisplayName("5. Fails with 403 when Farmer B tries to report mortality on Farmer A's animal")
  void testCreateMortalityReport_UnauthorizedOwnership() throws Exception {
    String payload =
        """
        {
          "animalId": "%s",
          "causeCategory": "ACCIDENT_INJURY"
        }
        """
            .formatted(testAnimalA.id());

    // Farmer B tries to report Farmer A's animal
    mockMvc
        .perform(
            post("/api/v1/mortalities")
                .header("Authorization", "Bearer " + farmerTokenB)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.success").value(false));
  }

  @Test
  @DisplayName("6. Fails with 403 when non-farmer role (VET) attempts to report farmer mortality")
  void testCreateMortalityReport_NonFarmerRole() throws Exception {
    String payload =
        """
        {
          "animalId": "%s",
          "causeCategory": "ACCIDENT_INJURY"
        }
        """
            .formatted(testAnimalA.id());

    mockMvc
        .perform(
            post("/api/v1/mortalities")
                .header("Authorization", "Bearer " + vetToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("7. Fails with 409 Conflict when submitting mortality for an already deceased animal")
  void testCreateMortalityReport_AlreadyDeceasedConflict() throws Exception {
    String payload =
        """
        {
          "animalId": "%s",
          "causeCategory": "OTHER",
          "notes": "First report"
        }
        """
            .formatted(testAnimalA.id());

    // First report succeeds
    mockMvc
        .perform(
            post("/api/v1/mortalities")
                .header("Authorization", "Bearer " + farmerTokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isCreated());

    // Second report on same animal must be rejected with 409 Conflict
    mockMvc
        .perform(
            post("/api/v1/mortalities")
                .header("Authorization", "Bearer " + farmerTokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.success").value(false));
  }

  @Test
  @DisplayName("8. Idempotent submission: same Idempotency-Key returns cached response without duplicate records")
  void testCreateMortalityReport_IdempotencyDuplicateKey() throws Exception {
    String idempotencyKey = "test-idemp-" + UUID.randomUUID();
    String payload =
        """
        {
          "animalId": "%s",
          "causeCategory": "ACCIDENT_INJURY",
          "notes": "Fell from cliff"
        }
        """
            .formatted(testAnimalA.id());

    // First submission
    MvcResult result1 =
        mockMvc
            .perform(
                post("/api/v1/mortalities")
                    .header("Authorization", "Bearer " + farmerTokenA)
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andReturn();

    // Duplicate submission with same key
    MvcResult result2 =
        mockMvc
            .perform(
                post("/api/v1/mortalities")
                    .header("Authorization", "Bearer " + farmerTokenA)
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andReturn();

    // Confirm exactly ONE mortality record exists for this animal
    List<AnimalMortalityEvent> events = mortalityRepository.findAll();
    long countForAnimal =
        events.stream().filter(e -> e.getAnimal().getId().equals(testAnimalA.id())).count();
    assertThat(countForAnimal).isEqualTo(1);
  }

  @Test
  @DisplayName("9. Retrieves mortality report by ID and by Animal ID")
  void testGetMortalityByIdAndAnimalId() throws Exception {
    String payload =
        """
        {
          "animalId": "%s",
          "causeCategory": "SUSPECTED_DISEASE",
          "diseaseName": "Rabies"
        }
        """
            .formatted(testAnimalA.id());

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/mortalities")
                    .header("Authorization", "Bearer " + farmerTokenA)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
            .andExpect(status().isCreated())
            .andReturn();

    String content = result.getResponse().getContentAsString();
    UUID mortalityId = UUID.fromString(objectMapper.readTree(content).path("data").path("id").asText());

    // GET by mortality ID
    mockMvc
        .perform(
            get("/api/v1/mortalities/" + mortalityId)
                .header("Authorization", "Bearer " + farmerTokenA))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(mortalityId.toString()))
        .andExpect(jsonPath("$.data.diseaseName").value("Rabies"));

    // GET by animal ID
    mockMvc
        .perform(
            get("/api/v1/mortalities/animal/" + testAnimalA.id())
                .header("Authorization", "Bearer " + farmerTokenA))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.animalId").value(testAnimalA.id().toString()));
  }

  @Test
  @DisplayName("10. Lists farmer's mortality reports with pagination")
  void testListFarmerMortalities() throws Exception {
    String payload =
        """
        {
          "animalId": "%s",
          "causeCategory": "OTHER",
          "notes": "Old age"
        }
        """
            .formatted(testAnimalA.id());

    mockMvc
        .perform(
            post("/api/v1/mortalities")
                .header("Authorization", "Bearer " + farmerTokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            get("/api/v1/mortalities")
                .header("Authorization", "Bearer " + farmerTokenA))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.content").isArray())
        .andExpect(jsonPath("$.data.totalElements").value(1));
  }
}
