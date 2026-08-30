package app.vetra.animal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.animal.dto.AnimalHealthRecordDto;
import app.vetra.animal.dto.AnimalHealthStatusDto;
import app.vetra.animal.dto.CreateHealthRecordRequest;
import app.vetra.animal.repository.AnimalHealthRecordRepository;
import app.vetra.animal.repository.AnimalRepository;
import app.vetra.animal.service.AnimalHealthRecordService;
import app.vetra.auth.repository.FarmerProfileRepository;
import app.vetra.auth.repository.UserRepository;
import app.vetra.auth.repository.VetProfileRepository;
import app.vetra.infrastructure.exception.UnauthorizedResourceAccessException;
import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.entity.FarmerProfile;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.infrastructure.persistence.enums.AnimalGender;
import app.vetra.infrastructure.persistence.enums.HealthRecordSource;
import app.vetra.infrastructure.persistence.enums.HealthRecordType;
import app.vetra.infrastructure.persistence.enums.Species;
import app.vetra.infrastructure.persistence.enums.UserRole;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/** Integration and unit tests for AnimalHealthRecordService and Lifetime Timeline rules. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:animal_health_record_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
class AnimalHealthRecordServiceTest {

  @Autowired private AnimalHealthRecordService healthRecordService;
  @Autowired private AnimalHealthRecordRepository healthRecordRepository;
  @Autowired private AnimalRepository animalRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private FarmerProfileRepository farmerProfileRepository;
  @Autowired private VetProfileRepository vetProfileRepository;

  private User farmerUser1;
  private User farmerUser2;
  private User vetUser;
  private Animal animal1;

  @BeforeEach
  void setUp() {
    farmerUser1 =
        userRepository.save(
            User.builder()
                .email("farmer1@vetra.app")
                .passwordHash("hashed")
                .role(UserRole.FARMER)
                .phone("+919100000001")
                .isActive(true)
                .build());

    FarmerProfile farmerProfile1 =
        farmerProfileRepository.save(
            FarmerProfile.builder()
                .user(farmerUser1)
                .fullName("Ramesh Patil")
                .farmName("Patil Dairy Farm")
                .village("Baramati")
                .district("Pune")
                .state("Maharashtra")
                .build());

    farmerUser2 =
        userRepository.save(
            User.builder()
                .email("farmer2@vetra.app")
                .passwordHash("hashed")
                .role(UserRole.FARMER)
                .phone("+919100000002")
                .isActive(true)
                .build());

    farmerProfileRepository.save(
        FarmerProfile.builder()
            .user(farmerUser2)
            .fullName("Suresh Shinde")
            .farmName("Other Farm")
            .village("Haveli")
            .district("Pune")
            .state("Maharashtra")
            .build());

    vetUser =
        userRepository.save(
            User.builder()
                .email("dr.ananya@vetra.app")
                .passwordHash("hashed")
                .role(UserRole.VETERINARIAN)
                .phone("+919876548001")
                .isActive(true)
                .build());

    vetProfileRepository.save(
        VetProfile.builder()
            .user(vetUser)
            .fullName("Dr. Ananya Roy")
            .registrationNumber("VET-12345")
            .clinicName("Roy Animal Hospital")
            .build());

    animal1 =
        animalRepository.save(
            Animal.builder()
                .farmer(farmerProfile1)
                .animalName("Gauri")
                .tagNumber("MH-PUN-001")
                .species(Species.CATTLE)
                .breed("Gir")
                .gender(AnimalGender.FEMALE)
                .birthDate(LocalDate.of(2022, 1, 1))
                .build());
  }

  @Test
  @DisplayName("Farmer can create and view timeline records for their own animal")
  void testFarmerCreateAndGetTimeline() {
    CreateHealthRecordRequest req =
        new CreateHealthRecordRequest(
            HealthRecordType.VACCINATION,
            HealthRecordSource.FARMER,
            "FMD Vaccination",
            "Administered annual Foot and Mouth Disease vaccine dose",
            null,
            null,
            "FMD Vaccine 5ml",
            null,
            null,
            LocalDateTime.now().minusDays(10));

    AnimalHealthRecordDto created =
        healthRecordService.createRecord(farmerUser1.getEmail(), animal1.getId(), req);

    assertNotNull(created);
    assertEquals(HealthRecordType.VACCINATION, created.recordType());
    assertEquals(HealthRecordSource.FARMER, created.source());
    assertEquals("FMD Vaccination", created.title());

    List<AnimalHealthRecordDto> timeline =
        healthRecordService.getAnimalTimeline(farmerUser1.getEmail(), animal1.getId());

    assertFalse(timeline.isEmpty());
    assertEquals(1, timeline.size());
    assertEquals("FMD Vaccination", timeline.get(0).title());
  }

  @Test
  @DisplayName("Multiple timeline records are returned newest first")
  void testTimelineOrderingNewestFirst() {
    CreateHealthRecordRequest reqOld =
        new CreateHealthRecordRequest(
            HealthRecordType.VACCINATION,
            HealthRecordSource.FARMER,
            "Old Vaccination",
            "Initial dose",
            null,
            null,
            "Vaccine",
            null,
            null,
            LocalDateTime.now().minusMonths(2));

    CreateHealthRecordRequest reqMid =
        new CreateHealthRecordRequest(
            HealthRecordType.VET_CONSULTATION,
            HealthRecordSource.VETERINARIAN,
            "Routine Checkup",
            "General health evaluation",
            "None",
            "Healthy",
            "Vitamins",
            null,
            "Dr. Ananya Roy",
            LocalDateTime.now().minusWeeks(1));

    CreateHealthRecordRequest reqRecent =
        new CreateHealthRecordRequest(
            HealthRecordType.AI_SCREENING,
            HealthRecordSource.AI_ADVISOR,
            "AI Health Screening",
            "Farmer reported minor cough",
            "Cough",
            "Mild Respiratory Irritation",
            "Warm fluids and rest",
            null,
            null,
            LocalDateTime.now());

    healthRecordService.createRecord(farmerUser1.getEmail(), animal1.getId(), reqOld);
    healthRecordService.createRecord(vetUser.getEmail(), animal1.getId(), reqMid);
    healthRecordService.createRecord(farmerUser1.getEmail(), animal1.getId(), reqRecent);

    List<AnimalHealthRecordDto> timeline =
        healthRecordService.getAnimalTimeline(farmerUser1.getEmail(), animal1.getId());

    assertEquals(3, timeline.size());
    assertEquals("AI Health Screening", timeline.get(0).title());
    assertEquals("Routine Checkup", timeline.get(1).title());
    assertEquals("Old Vaccination", timeline.get(2).title());
  }

  @Test
  @DisplayName("Unauthorized farmer cannot view or create health records for another farmer's animal")
  void testUnauthorizedFarmerAccessForbidden() {
    assertThrows(
        UnauthorizedResourceAccessException.class,
        () -> healthRecordService.getAnimalTimeline(farmerUser2.getEmail(), animal1.getId()));

    CreateHealthRecordRequest req =
        new CreateHealthRecordRequest(
            HealthRecordType.OBSERVATION,
            HealthRecordSource.FARMER,
            "Unauthorized Observation",
            "Intruder note",
            null,
            null,
            null,
            null,
            null,
            LocalDateTime.now());

    assertThrows(
        UnauthorizedResourceAccessException.class,
        () -> healthRecordService.createRecord(farmerUser2.getEmail(), animal1.getId(), req));
  }

  @Test
  @DisplayName("Latest health status is dynamically computed from recent timeline events")
  void testGetLatestHealthStatus() {
    // Initial state: No records -> HEALTHY
    AnimalHealthStatusDto initialStatus =
        healthRecordService.getLatestHealthStatus(farmerUser1.getEmail(), animal1.getId());
    assertEquals("HEALTHY", initialStatus.status());

    // Add diagnosis event
    CreateHealthRecordRequest diagReq =
        new CreateHealthRecordRequest(
            HealthRecordType.DIAGNOSIS,
            HealthRecordSource.VETERINARIAN,
            "Mastitis Detected",
            "Clinical mastitis symptoms observed",
            "Swelling, fever",
            "Bovine Mastitis",
            "Antibiotics prescribed",
            null,
            "Dr. Ananya Roy",
            LocalDateTime.now());

    healthRecordService.createRecord(vetUser.getEmail(), animal1.getId(), diagReq);

    AnimalHealthStatusDto updatedStatus =
        healthRecordService.getLatestHealthStatus(farmerUser1.getEmail(), animal1.getId());
    assertEquals("ATTENTION_REQUIRED", updatedStatus.status());
    assertTrue(updatedStatus.statusSummary().contains("Bovine Mastitis"));
  }
}
