package app.vetra.animal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.vetra.animal.dto.CreateHealthRecordRequest;
import app.vetra.animal.repository.AnimalHealthRecordRepository;
import app.vetra.animal.repository.AnimalRepository;
import app.vetra.appointment.repository.AppointmentRepository;
import app.vetra.auth.repository.FarmerProfileRepository;
import app.vetra.auth.repository.UserRepository;
import app.vetra.auth.repository.VetProfileRepository;
import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.entity.AnimalHealthRecord;
import app.vetra.infrastructure.persistence.entity.Appointment;
import app.vetra.infrastructure.persistence.entity.FarmerProfile;
import app.vetra.infrastructure.persistence.entity.MedicalRecord;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.infrastructure.persistence.enums.AnimalGender;
import app.vetra.infrastructure.persistence.enums.AppointmentStatus;
import app.vetra.infrastructure.persistence.enums.HealthRecordSource;
import app.vetra.infrastructure.persistence.enums.HealthRecordType;
import app.vetra.infrastructure.persistence.enums.Species;
import app.vetra.infrastructure.persistence.enums.UserRole;
import app.vetra.infrastructure.persistence.enums.VisitType;
import app.vetra.medicalrecord.dto.CreateMedicalRecordRequest;
import app.vetra.medicalrecord.dto.MedicalRecordResponse;
import app.vetra.medicalrecord.repository.MedicalRecordRepository;
import app.vetra.medicalrecord.service.MedicalRecordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
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
  @Autowired private VetProfileRepository vetProfileRepository;
  @Autowired private AppointmentRepository appointmentRepository;
  @Autowired private MedicalRecordRepository medicalRecordRepository;
  @Autowired private AnimalHealthRecordRepository animalHealthRecordRepository;
  @Autowired private MedicalRecordService medicalRecordService;

  private FarmerProfile farmerProfile;
  private Animal animal;
  private User vetUser;
  private VetProfile vetProfile;

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

    farmerProfile =
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

    vetUser =
        userRepository.save(
            User.builder()
                .email("dr.patil@vetra.app")
                .passwordHash("hashed")
                .role(UserRole.VETERINARIAN)
                .phone("+919111222333")
                .isActive(true)
                .build());

    vetProfile =
        vetProfileRepository.save(
            VetProfile.builder()
                .user(vetUser)
                .fullName("Dr. Rajesh Patil")
                .registrationNumber("VET-MH-99881")
                .qualification("BVSc & AH")
                .clinicName("Nashik Animal Care")
                .isAvailable(true)
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
  @WithMockUser(username = "owner.farmer@vetra.app", roles = "FARMER")
  @DisplayName("createMedicalRecord() saves MedicalRecord, creates AnimalHealthRecord, and surfaces in timeline")
  void testCreateMedicalRecordSynchronizesAnimalHealthRecordAndTimeline() throws Exception {
    Appointment appointment =
        appointmentRepository.save(
            Appointment.builder()
                .farmer(farmerProfile)
                .veterinarian(vetProfile)
                .animal(animal)
                .appointmentDate(LocalDate.now())
                .appointmentTime(LocalTime.of(10, 0))
                .visitType(VisitType.GENERAL_CHECKUP)
                .reason("Cough and fever")
                .status(AppointmentStatus.COMPLETED)
                .build());

    CreateMedicalRecordRequest request =
        new CreateMedicalRecordRequest(
            appointment.getId(),
            "Bovine Respiratory Disease",
            "Coughing, nasal discharge",
            "Enrofloxacin 10%",
            "Enrofloxacin 10ml IM for 3 days",
            new BigDecimal("350.00"),
            new BigDecimal("39.8"),
            LocalDate.now().plusDays(5),
            "Isolate animal in well-ventilated pen");

    MedicalRecordResponse mrResponse =
        medicalRecordService.createMedicalRecord(vetUser.getEmail(), request);

    assertNotNull(mrResponse);
    assertEquals("Bovine Respiratory Disease", mrResponse.diagnosis());

    // 1. Verify canonical MedicalRecord exists
    assertTrue(medicalRecordRepository.existsByAppointmentId(appointment.getId()));

    // 2. Verify AnimalHealthRecord was synchronized with deterministic keys
    List<AnimalHealthRecord> healthRecords =
        animalHealthRecordRepository.findByAnimalIdOrderByRecordedAtDesc(animal.getId());
    assertEquals(1, healthRecords.size());
    AnimalHealthRecord ahr = healthRecords.get(0);
    assertEquals(animal.getId(), ahr.getAnimal().getId());
    assertEquals(HealthRecordType.VET_CONSULTATION, ahr.getRecordType());
    assertEquals(HealthRecordSource.VETERINARIAN, ahr.getSource());
    assertTrue(ahr.getTitle().contains("Bovine Respiratory Disease"));
    assertEquals("Bovine Respiratory Disease", ahr.getDiagnosis());
    assertEquals("Coughing, nasal discharge", ahr.getSymptoms());
    assertTrue(ahr.getTreatment().contains("Enrofloxacin 10%"));
    assertTrue(ahr.getTreatment().contains("Enrofloxacin 10ml IM for 3 days"));
    assertEquals(vetProfile.getId(), ahr.getVeterinarianId());
    assertEquals("Dr. Rajesh Patil", ahr.getVeterinarianName());
    assertEquals(mrResponse.id(), ahr.getMedicalRecordId());
    assertEquals(appointment.getId(), ahr.getAppointmentId());

    // 3. Verify GET /api/v1/animals/{animalId}/health-records returns the record without duplicates
    mockMvc
        .perform(get("/api/v1/animals/" + animal.getId() + "/health-records"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].recordType").value("VET_CONSULTATION"))
        .andExpect(jsonPath("$.data[0].source").value("VETERINARIAN"))
        .andExpect(jsonPath("$.data[0].diagnosis").value("Bovine Respiratory Disease"))
        .andExpect(jsonPath("$.data[0].veterinarianName").value("Dr. Rajesh Patil"))
        .andExpect(jsonPath("$.data[0].medicalRecordId").value(mrResponse.id().toString()))
        .andExpect(jsonPath("$.data[0].appointmentId").value(appointment.getId().toString()));
  }

  @Test
  @WithMockUser(username = "owner.farmer@vetra.app", roles = "FARMER")
  @DisplayName("Legacy MedicalRecord without AnimalHealthRecord is surfaced on timeline without duplicates")
  void testLegacyMedicalRecordSurfacesOnTimelineWithoutDuplicates() throws Exception {
    // Save legacy MedicalRecord directly with NO AnimalHealthRecord
    MedicalRecord legacy =
        medicalRecordRepository.save(
            MedicalRecord.builder()
                .animal(animal)
                .farmer(farmerProfile)
                .veterinarian(vetProfile)
                .diagnosis("Foot and Mouth Disease")
                .symptoms("Blisters on hooves, salivation")
                .treatment("Antiseptic wash and ring vaccination")
                .prescription("Potassium permanganate wash 1:1000")
                .notes("Recorded during previous field camp")
                .build());

    // Verify animal_health_records table is empty for this animal
    List<AnimalHealthRecord> existingAhrs =
        animalHealthRecordRepository.findByAnimalIdOrderByRecordedAtDesc(animal.getId());
    assertTrue(existingAhrs.isEmpty());

    // Timeline endpoint should surface the legacy record
    mockMvc
        .perform(get("/api/v1/animals/" + animal.getId() + "/health-records"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].diagnosis").value("Foot and Mouth Disease"))
        .andExpect(jsonPath("$.data[0].recordType").value("VET_CONSULTATION"))
        .andExpect(jsonPath("$.data[0].source").value("VETERINARIAN"))
        .andExpect(jsonPath("$.data[0].veterinarianName").value("Dr. Rajesh Patil"));

    // Now create a new consultation via medicalRecordService
    Appointment newAppt =
        appointmentRepository.save(
            Appointment.builder()
                .farmer(farmerProfile)
                .veterinarian(vetProfile)
                .animal(animal)
                .appointmentDate(LocalDate.now())
                .appointmentTime(LocalTime.of(14, 0))
                .visitType(VisitType.GENERAL_CHECKUP)
                .reason("Follow-up checkup")
                .status(AppointmentStatus.COMPLETED)
                .build());

    CreateMedicalRecordRequest request =
        new CreateMedicalRecordRequest(
            newAppt.getId(),
            "Complete Recovery Checkup",
            "No active symptoms",
            "Supportive vitamins",
            "Mineral mixture 50g daily",
            new BigDecimal("355.00"),
            new BigDecimal("38.5"),
            null,
            "Healthy recovery");

    medicalRecordService.createMedicalRecord(vetUser.getEmail(), request);

    // Timeline should now contain exactly 2 records: 1 legacy + 1 new (no duplicates)
    mockMvc
        .perform(get("/api/v1/animals/" + animal.getId() + "/health-records"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(jsonPath("$.data[0].diagnosis").value("Complete Recovery Checkup"))
        .andExpect(jsonPath("$.data[1].diagnosis").value("Foot and Mouth Disease"));
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
