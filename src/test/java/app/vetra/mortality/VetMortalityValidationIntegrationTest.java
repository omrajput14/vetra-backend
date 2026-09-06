package app.vetra.mortality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.vetra.animal.dto.AnimalResponse;
import app.vetra.animal.dto.CreateAnimalRequest;
import app.vetra.animal.service.AnimalService;
import app.vetra.auth.dto.AuthResponse;
import app.vetra.auth.dto.FarmerRegisterRequest;
import app.vetra.auth.dto.LoginRequest;
import app.vetra.auth.dto.VetRegisterRequest;
import app.vetra.auth.repository.UserRepository;
import app.vetra.auth.repository.VetProfileRepository;
import app.vetra.auth.service.AuthService;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.infrastructure.persistence.enums.AnimalGender;
import app.vetra.infrastructure.persistence.enums.Species;
import app.vetra.infrastructure.persistence.enums.VerificationStatus;
import app.vetra.mortality.dto.ConfirmMortalityRequest;
import app.vetra.mortality.dto.CreateMortalityReportRequest;
import app.vetra.mortality.dto.RejectMortalityRequest;
import app.vetra.mortality.enums.MortalityCauseCategory;
import app.vetra.mortality.repository.AnimalMortalityEventRepository;
import app.vetra.mortality.repository.MortalityReviewAuditRepository;
import app.vetra.mortality.service.MortalityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
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
 * Integration test verifying Phase 2: Veterinarian Mortality Validation, Case Inbox,
 * Cause Overriding, Audit Trails, and Role Security.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:vetra_vet_mortality_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
class VetMortalityValidationIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AuthService authService;
  @Autowired private AnimalService animalService;
  @Autowired private MortalityService mortalityService;
  @Autowired private AnimalMortalityEventRepository mortalityRepository;
  @Autowired private MortalityReviewAuditRepository auditRepository;
  @Autowired private VetProfileRepository vetProfileRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private ObjectMapper objectMapper;

  private String farmerToken;
  private String farmerEmail;
  private String vetToken;
  private String vetEmail;
  private String unverifiedVetToken;
  private AnimalResponse testAnimal;

  @BeforeEach
  void setUp() {
    long timestamp = System.currentTimeMillis();

    // Register Farmer in Pune
    farmerEmail = "farmer." + timestamp + "@vetra.app";
    authService.registerFarmer(
        new FarmerRegisterRequest(
            farmerEmail,
            "9898" + (timestamp % 1000000),
            "Password@123",
            "Anand Rao",
            "Rao Dairy",
            "Haveli",
            "Pune",
            "Pune",
            "Maharashtra",
            18.5204,
            73.8567,
            8,
            "en"));
    AuthResponse farmerLogin =
        authService.loginFarmer(new LoginRequest(farmerEmail, "Password@123"));
    farmerToken = farmerLogin.accessToken();

    // Register Verified Vet in Pune
    vetEmail = "vet." + timestamp + "@vetra.app";
    authService.registerVet(
        new VetRegisterRequest(
            vetEmail,
            "9797" + (timestamp % 1000000),
            "Password@123",
            "Dr. Sneha Joshi",
            "VET-REG-" + timestamp,
            "BVSc & AH",
            "Large Animal Medicine",
            "Pune Vet Clinic",
            "Haveli Road",
            "Haveli",
            "Pune",
            "Pune",
            "Maharashtra",
            6,
            18.5210,
            73.8570,
            "en"));
    User vetUser = userRepository.findByIdentifier(vetEmail).orElseThrow();
    VetProfile vetProfile = vetProfileRepository.findByUser(vetUser).orElseThrow();
    vetProfile.setVerificationStatus(VerificationStatus.VERIFIED);
    vetProfileRepository.save(vetProfile);

    AuthResponse vetLogin = authService.loginVet(new LoginRequest(vetEmail, "Password@123"));
    vetToken = vetLogin.accessToken();

    // Register Unverified Vet
    String unverifiedVetEmail = "unverified.vet." + timestamp + "@vetra.app";
    authService.registerVet(
        new VetRegisterRequest(
            unverifiedVetEmail,
            "9696" + (timestamp % 1000000),
            "Password@123",
            "Dr. Unverified",
            "VET-UNVERIFIED-" + timestamp,
            "BVSc",
            "General Practice",
            "Clinic",
            "Road",
            "Haveli",
            "Pune",
            "Pune",
            "Maharashtra",
            2,
            18.5220,
            73.8580,
            "en"));
    AuthResponse unverifiedLogin =
        authService.loginVet(new LoginRequest(unverifiedVetEmail, "Password@123"));
    unverifiedVetToken = unverifiedLogin.accessToken();

    // Register Animal for Farmer
    testAnimal =
        animalService.createAnimal(
            farmerEmail,
            new CreateAnimalRequest(
                "Gauri",
                "TAG-" + timestamp,
                "QR-" + timestamp,
                Species.CATTLE,
                "Gir",
                AnimalGender.FEMALE,
                LocalDate.now().minusYears(3),
                null));
  }

  @Test
  @DisplayName("Verified vet views referred pending cases, confirms diagnosis, and records audit trail")
  void testVetPendingInboxAndConfirmationFlow() throws Exception {
    // 1. Farmer reports animal mortality
    CreateMortalityReportRequest reportRequest =
        new CreateMortalityReportRequest(
            testAnimal.id(),
            MortalityCauseCategory.UNKNOWN,
            "Animal was found down in the morning",
            null,
            false,
            null,
            "Sudden recumbency",
            Instant.now(),
            18.5204,
            73.8567,
            10.0);

    MvcResult reportResult =
        mockMvc
            .perform(
                post("/api/v1/mortalities")
                    .header("Authorization", "Bearer " + farmerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(reportRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
            .andReturn();

    String reportResponseStr = reportResult.getResponse().getContentAsString();
    UUID mortalityId =
        UUID.fromString(objectMapper.readTree(reportResponseStr).path("data").path("id").asText());

    // 2. Veterinarian views pending inbox
    mockMvc
        .perform(
            get("/api/v1/mortalities/pending")
                .header("Authorization", "Bearer " + vetToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.content").isArray())
        .andExpect(jsonPath("$.data.content[0].id").value(mortalityId.toString()))
        .andExpect(jsonPath("$.data.content[0].tagNumber").value(testAnimal.tagNumber()));

    // 3. Veterinarian confirms mortality with specific clinical diagnosis and post-mortem flag
    ConfirmMortalityRequest confirmRequest =
        new ConfirmMortalityRequest(
            MortalityCauseCategory.KNOWN_DISEASE,
            "Anthrax",
            "Field investigation confirmed uncoagulated tarry blood discharge and peripheral smear positive.",
            true);

    mockMvc
        .perform(
            post("/api/v1/mortalities/" + mortalityId + "/confirm")
                .header("Authorization", "Bearer " + vetToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(confirmRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
        .andExpect(jsonPath("$.data.vetReviewedByName").value("Dr. Sneha Joshi"))
        .andExpect(jsonPath("$.data.vetCauseCategory").value("KNOWN_DISEASE"))
        .andExpect(jsonPath("$.data.vetDiseaseName").value("Anthrax"))
        .andExpect(jsonPath("$.data.postMortemConducted").value(true));

    // 4. Verify immutable audit log
    mockMvc
        .perform(
            get("/api/v1/mortalities/" + mortalityId + "/audits")
                .header("Authorization", "Bearer " + vetToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data[0].action").value("CONFIRM"))
        .andExpect(jsonPath("$.data[0].newStatus").value("CONFIRMED"))
        .andExpect(jsonPath("$.data[0].veterinarianName").value("Dr. Sneha Joshi"))
        .andExpect(jsonPath("$.data[0].confirmedCauseCategory").value("KNOWN_DISEASE"));

    // 5. Test idempotency: submitting confirmation again returns existing confirmed record
    mockMvc
        .perform(
            post("/api/v1/mortalities/" + mortalityId + "/confirm")
                .header("Authorization", "Bearer " + vetToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(confirmRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

    assertThat(auditRepository.findByMortalityEventIdOrderByCreatedAtAsc(mortalityId)).hasSize(1);
  }

  @Test
  @DisplayName("Veterinarian rejects false mortality report with clinical rationale")
  void testVetRejectionFlow() throws Exception {
    long timestamp = System.currentTimeMillis();
    AnimalResponse animal2 =
        animalService.createAnimal(
            farmerEmail,
            new CreateAnimalRequest(
                "Lakshmi",
                "TAG2-" + timestamp,
                "QR2-" + timestamp,
                Species.BUFFALO,
                "Murrah",
                AnimalGender.FEMALE,
                LocalDate.now().minusYears(4),
                null));

    CreateMortalityReportRequest reportRequest =
        new CreateMortalityReportRequest(
            animal2.id(),
            MortalityCauseCategory.ACCIDENT_INJURY,
            "Reported struck by vehicle",
            null,
            false,
            null,
            null,
            Instant.now(),
            18.5204,
            73.8567,
            10.0);

    MvcResult reportResult =
        mockMvc
            .perform(
                post("/api/v1/mortalities")
                    .header("Authorization", "Bearer " + farmerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(reportRequest)))
            .andExpect(status().isCreated())
            .andReturn();

    UUID mortalityId =
        UUID.fromString(
            objectMapper
                .readTree(reportResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asText());

    RejectMortalityRequest rejectRequest =
        new RejectMortalityRequest(
            "Animal was inspected on-site and confirmed fully alive with minor superficial abrasions.",
            "Animal is standing and ruminating normally.");

    mockMvc
        .perform(
            post("/api/v1/mortalities/" + mortalityId + "/reject")
                .header("Authorization", "Bearer " + vetToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rejectRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.status").value("REJECTED"))
        .andExpect(jsonPath("$.data.vetReviewedByName").value("Dr. Sneha Joshi"))
        .andExpect(jsonPath("$.data.vetRejectionReason").value(rejectRequest.rejectionReason()));

    // Verify audit log has REJECT action
    mockMvc
        .perform(
            get("/api/v1/mortalities/" + mortalityId + "/audits")
                .header("Authorization", "Bearer " + farmerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].action").value("REJECT"))
        .andExpect(jsonPath("$.data[0].newStatus").value("REJECTED"));
  }

  @Test
  @DisplayName("Role enforcement: Farmer cannot confirm, unverified vet cannot confirm")
  void testRoleAndVerificationEnforcement() throws Exception {
    CreateMortalityReportRequest reportRequest =
        new CreateMortalityReportRequest(
            testAnimal.id(),
            MortalityCauseCategory.SUSPECTED_DISEASE,
            "Severe tympany",
            null,
            false,
            null,
            null,
            Instant.now(),
            18.5204,
            73.8567,
            10.0);

    MvcResult reportResult =
        mockMvc
            .perform(
                post("/api/v1/mortalities")
                    .header("Authorization", "Bearer " + farmerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(reportRequest)))
            .andExpect(status().isCreated())
            .andReturn();

    UUID mortalityId =
        UUID.fromString(
            objectMapper
                .readTree(reportResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asText());

    ConfirmMortalityRequest confirmRequest =
        new ConfirmMortalityRequest(MortalityCauseCategory.SUSPECTED_DISEASE, null, "Confirmed bloat", false);

    // Farmer attempting to confirm -> 403 Forbidden
    mockMvc
        .perform(
            post("/api/v1/mortalities/" + mortalityId + "/confirm")
                .header("Authorization", "Bearer " + farmerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(confirmRequest)))
        .andExpect(status().isForbidden());

    // Unverified veterinarian attempting to confirm -> 403 Forbidden
    mockMvc
        .perform(
            post("/api/v1/mortalities/" + mortalityId + "/confirm")
                .header("Authorization", "Bearer " + unverifiedVetToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(confirmRequest)))
        .andExpect(status().isForbidden());
  }
}
