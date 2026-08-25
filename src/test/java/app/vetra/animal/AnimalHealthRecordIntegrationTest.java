package app.vetra.animal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.vetra.animal.dto.CreateHealthRecordRequest;
import app.vetra.animal.repository.AnimalRepository;
import app.vetra.auth.repository.FarmerProfileRepository;
import app.vetra.auth.repository.UserRepository;
import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.entity.FarmerProfile;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.enums.AnimalGender;
import app.vetra.infrastructure.persistence.enums.HealthRecordSource;
import app.vetra.infrastructure.persistence.enums.HealthRecordType;
import app.vetra.infrastructure.persistence.enums.Species;
import app.vetra.infrastructure.persistence.enums.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** End-to-end integration tests for Animal Health Timeline REST endpoints. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:animal_health_endpoint_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
class AnimalHealthRecordIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private AnimalRepository animalRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private FarmerProfileRepository farmerProfileRepository;

  private Animal animal;

  @BeforeEach
  void setUp() {
    User farmerUser =
        userRepository.save(
            User.builder()
                .email("owner.farmer@vetra.app")
                .passwordHash("hashed")
                .role(UserRole.FARMER)
                .phone("+919988776655")
                .isActive(true)
                .build());

    FarmerProfile farmerProfile =
        farmerProfileRepository.save(
            FarmerProfile.builder()
                .user(farmerUser)
                .fullName("Ram Kumar")
                .farmName("Shree Ram Dairy")
                .village("Nashik")
                .district("Nashik")
                .state("Maharashtra")
                .build());

    animal =
        animalRepository.save(
            Animal.builder()
                .farmer(farmerProfile)
                .animalName("Sundari")
                .tagNumber("MH-NSK-101")
                .species(Species.CATTLE)
                .breed("Gir")
                .gender(AnimalGender.FEMALE)
                .birthDate(LocalDate.of(2023, 2, 15))
                .build());
  }

  @Test
  @WithMockUser(username = "owner.farmer@vetra.app", roles = "FARMER")
  @DisplayName("POST & GET /api/v1/animals/{animalId}/health-records succeeds for animal owner")
  void testCreateAndGetHealthRecords() throws Exception {
    CreateHealthRecordRequest request =
        new CreateHealthRecordRequest(
            HealthRecordType.VACCINATION,
            HealthRecordSource.FARMER,
            "Black Quarter Vaccine",
            "Annual preventive vaccination administered",
            null,
            null,
            "BQ Vaccine 2ml SQ",
            null,
            null,
            LocalDateTime.now());

    mockMvc
        .perform(
            post("/api/v1/animals/" + animal.getId() + "/health-records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.title").value("Black Quarter Vaccine"))
        .andExpect(jsonPath("$.data.recordType").value("VACCINATION"))
        .andExpect(jsonPath("$.data.source").value("FARMER"));

    mockMvc
        .perform(get("/api/v1/animals/" + animal.getId() + "/health-records"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].title").value("Black Quarter Vaccine"))
        .andExpect(jsonPath("$.data[0].treatment").value("BQ Vaccine 2ml SQ"));
  }

  @Test
  @WithMockUser(username = "stranger.farmer@vetra.app", roles = "FARMER")
  @DisplayName("GET /api/v1/animals/{animalId}/health-records returns 403 Forbidden for unauthorized user")
  void testGetHealthRecordsForbiddenForStranger() throws Exception {
    User stranger =
        userRepository.save(
            User.builder()
                .email("stranger.farmer@vetra.app")
                .passwordHash("hashed")
                .role(UserRole.FARMER)
                .phone("+919000000099")
                .isActive(true)
                .build());

    farmerProfileRepository.save(
        FarmerProfile.builder()
            .user(stranger)
            .fullName("Stranger Farmer")
            .farmName("Stranger Farm")
            .village("Nashik")
            .district("Nashik")
            .state("Maharashtra")
            .build());

    mockMvc
        .perform(get("/api/v1/animals/" + animal.getId() + "/health-records"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "owner.farmer@vetra.app", roles = "FARMER")
  @DisplayName("GET /api/v1/animals/{animalId}/health-records/latest-status returns dynamic status")
  void testGetLatestStatus() throws Exception {
    mockMvc
        .perform(get("/api/v1/animals/" + animal.getId() + "/health-records/latest-status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.status").value("HEALTHY"));
  }
}
