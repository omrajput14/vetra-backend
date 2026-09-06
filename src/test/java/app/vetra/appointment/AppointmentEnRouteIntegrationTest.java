package app.vetra.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.vetra.animal.dto.AnimalResponse;
import app.vetra.animal.dto.CreateAnimalRequest;
import app.vetra.animal.service.AnimalService;
import app.vetra.appointment.dto.CreateAppointmentRequest;
import app.vetra.appointment.dto.UpdateAppointmentLocationRequest;
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
import app.vetra.infrastructure.persistence.enums.VisitType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalTime;
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
 * Integration tests for Appointment EN_ROUTE lifecycle, live GPS updates,
 * and termination upon arrival.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:vetra_enroute_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
class AppointmentEnRouteIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AuthService authService;
  @Autowired private AnimalService animalService;
  @Autowired private UserRepository userRepository;
  @Autowired private VetProfileRepository vetProfileRepository;
  @Autowired private ObjectMapper objectMapper;

  private String farmerToken;
  private String farmerEmail;
  private String vetToken;
  private String vetEmail;
  private UUID vetProfileId;
  private AnimalResponse testAnimal;

  @BeforeEach
  void setUp() {
    long timestamp = System.currentTimeMillis();

    // Register Farmer (Pune - 18.5204, 73.8567)
    farmerEmail = "farmer.appt." + timestamp + "@vetra.app";
    authService.registerFarmer(
        new FarmerRegisterRequest(
            farmerEmail,
            "9888" + (timestamp % 1000000),
            "Password@123",
            "Vikram Shinde",
            "Shinde Farm",
            "Haveli",
            "Pune",
            "Pune",
            "Maharashtra",
            18.5204,
            73.8567,
            5,
            "en"));
    AuthResponse farmerLogin =
        authService.loginFarmer(new LoginRequest(farmerEmail, "Password@123"));
    farmerToken = farmerLogin.accessToken();

    // Register Verified Vet (10 km away - 18.6000, 73.8567)
    vetEmail = "vet.appt." + timestamp + "@vetra.app";
    authService.registerVet(
        new VetRegisterRequest(
            vetEmail,
            "9777" + (timestamp % 1000000),
            "Password@123",
            "Dr. Rajesh Deshmukh",
            "VET-DESH-" + timestamp,
            "BVSc & AH",
            "Bovine Medicine",
            "Haveli Clinic",
            "Main Rd",
            "Haveli",
            "Pune",
            "Pune",
            "Maharashtra",
            8,
            18.6000,
            73.8567,
            "en"));
    User vetUser = userRepository.findByIdentifier(vetEmail).orElseThrow();
    VetProfile vetProfile = vetProfileRepository.findByUser(vetUser).orElseThrow();
    vetProfile.setVerificationStatus(VerificationStatus.VERIFIED);
    vetProfileRepository.save(vetProfile);
    vetProfileId = vetProfile.getId();

    AuthResponse vetLogin = authService.loginVet(new LoginRequest(vetEmail, "Password@123"));
    vetToken = vetLogin.accessToken();

    // Register Animal
    testAnimal =
        animalService.createAnimal(
            farmerEmail,
            new CreateAnimalRequest(
                "Sundari",
                "TAG-APP-" + timestamp,
                "QR-APP-" + timestamp,
                Species.CATTLE,
                "Jersey",
                AnimalGender.FEMALE,
                LocalDate.now().minusYears(2),
                null));
  }

  @Test
  @DisplayName("Full lifecycle: CONFIRMED -> EN_ROUTE -> live tracking -> ARRIVED -> COMPLETED")
  void testEnRouteAndLiveTrackingLifecycle() throws Exception {
    // 1. Farmer requests appointment
    CreateAppointmentRequest createReq =
        new CreateAppointmentRequest(
            testAnimal.id(),
            vetProfileId,
            LocalDate.now().plusDays(1),
            LocalTime.of(10, 30),
            VisitType.GENERAL_CHECKUP,
            "General health inspection");

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/appointments")
                    .header("Authorization", "Bearer " + farmerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createReq)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andReturn();

    UUID apptId =
        UUID.fromString(
            objectMapper
                .readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asText());

    // 2. Vet confirms appointment
    mockMvc
        .perform(
            patch("/api/v1/appointments/" + apptId + "/confirm")
                .header("Authorization", "Bearer " + vetToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

    // Verify live location is NOT active while CONFIRMED
    mockMvc
        .perform(
            get("/api/v1/appointments/" + apptId + "/location")
                .header("Authorization", "Bearer " + farmerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.isLive").value(false));

    // 3. Vet departs -> EN_ROUTE
    mockMvc
        .perform(
            patch("/api/v1/appointments/" + apptId + "/en-route")
                .header("Authorization", "Bearer " + vetToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("EN_ROUTE"));

    // 4. Vet streams live coordinates (18.5500, 73.8567 - roughly 3.3 km from farmer)
    UpdateAppointmentLocationRequest locReq =
        new UpdateAppointmentLocationRequest(18.5500, 73.8567, 5.0);

    mockMvc
        .perform(
            post("/api/v1/appointments/" + apptId + "/location")
                .header("Authorization", "Bearer " + vetToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(locReq)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.isLive").value(true))
        .andExpect(jsonPath("$.data.latitude").value(18.5500))
        .andExpect(jsonPath("$.data.distanceKm").isNumber());

    // 5. Farmer views live location and distance
    MvcResult farmerLocResult =
        mockMvc
            .perform(
                get("/api/v1/appointments/" + apptId + "/location")
                    .header("Authorization", "Bearer " + farmerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.isLive").value(true))
            .andExpect(jsonPath("$.data.latitude").value(18.5500))
            .andExpect(jsonPath("$.data.longitude").value(73.8567))
            .andReturn();

    Double distKm =
        objectMapper
            .readTree(farmerLocResult.getResponse().getContentAsString())
            .path("data")
            .path("distanceKm")
            .asDouble();
    assertThat(distKm).isGreaterThan(0.0).isLessThan(10.0);

    // 6. Vet arrives at farmer's location -> ARRIVED
    mockMvc
        .perform(
            patch("/api/v1/appointments/" + apptId + "/arrive")
                .header("Authorization", "Bearer " + vetToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("ARRIVED"));

    // Live location tracking terminates upon arrival
    mockMvc
        .perform(
            get("/api/v1/appointments/" + apptId + "/location")
                .header("Authorization", "Bearer " + farmerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.isLive").value(false));

    // 7. Vet completes appointment with consultation notes
    mockMvc
        .perform(
            patch("/api/v1/appointments/" + apptId + "/complete")
                .header("Authorization", "Bearer " + vetToken)
                .param("notes", "Routine checkup complete. Prescribed calcium supplements."))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("COMPLETED"))
        .andExpect(
            jsonPath("$.data.veterinarianNotes")
                .value("Routine checkup complete. Prescribed calcium supplements."));
  }
}
