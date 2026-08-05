package app.vetra.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.vetra.animal.dto.AnimalResponse;
import app.vetra.animal.dto.CreateAnimalRequest;
import app.vetra.animal.service.AnimalService;
import app.vetra.appointment.dto.AppointmentResponse;
import app.vetra.appointment.dto.CreateAppointmentRequest;
import app.vetra.appointment.dto.UpdateAppointmentStatusRequest;
import app.vetra.appointment.repository.AppointmentRepository;
import app.vetra.appointment.service.AppointmentService;
import app.vetra.auth.dto.FarmerRegisterRequest;
import app.vetra.auth.dto.VetRegisterRequest;
import app.vetra.auth.repository.VetProfileRepository;
import app.vetra.auth.service.AuthService;
import app.vetra.dashboard.dto.DashboardResponse;
import app.vetra.dashboard.service.DashboardService;
import app.vetra.infrastructure.exception.BusinessRuleException;
import app.vetra.infrastructure.persistence.entity.Appointment;
import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.infrastructure.persistence.enums.AnimalGender;
import app.vetra.infrastructure.persistence.enums.AppointmentStatus;
import app.vetra.infrastructure.persistence.enums.Species;
import app.vetra.infrastructure.persistence.enums.VisitType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for AppointmentService state machine and business rules. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:vetra_appointment_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
class AppointmentServiceTest {

  @Autowired private AppointmentService appointmentService;
  @Autowired private AppointmentRepository appointmentRepository;
  @Autowired private AnimalService animalService;
  @Autowired private AuthService authService;
  @Autowired private VetProfileRepository vetProfileRepository;
  @Autowired private DashboardService dashboardService;

  @Test
  void testCompleteAppointmentLifecycleAndStateMachine() {
    registerUsersAndAnimal();
    VetProfile vetProfile = vetProfileRepository.findAll().get(0);

    CreateAnimalRequest createAnimalReq =
        new CreateAnimalRequest(
            "Bella",
            "TAG-BELL-1",
            "QR-BELL-1",
            Species.CATTLE,
            "Jersey",
            AnimalGender.FEMALE,
            LocalDate.of(2023, 1, 15),
            null);
    AnimalResponse animal = animalService.createAnimal("farmer_app@vetra.app", createAnimalReq);

    CreateAppointmentRequest appReq =
        new CreateAppointmentRequest(
            animal.id(),
            vetProfile.getId(),
            LocalDate.now().plusDays(2),
            LocalTime.of(10, 30),
            VisitType.GENERAL_CHECKUP,
            "Routine health inspection");

    AppointmentResponse createdApp =
        appointmentService.createAppointment("farmer_app@vetra.app", appReq);
    assertNotNull(createdApp.id());
    assertEquals(AppointmentStatus.PENDING, createdApp.status());

    DashboardResponse farmerDash = dashboardService.getDashboardMetrics("farmer_app@vetra.app");
    assertEquals(1, farmerDash.pendingAppointmentsCount());

    List<AppointmentResponse> vetList = appointmentService.listAppointments("vet_app@vetra.app");
    assertEquals(1, vetList.size());

    AppointmentResponse confirmed =
        appointmentService.confirmAppointment("vet_app@vetra.app", createdApp.id());
    assertEquals(AppointmentStatus.CONFIRMED, confirmed.status());

    AppointmentResponse completed =
        appointmentService.completeAppointment(
            "vet_app@vetra.app", createdApp.id(), "Animal is healthy.");
    assertEquals(AppointmentStatus.COMPLETED, completed.status());

    BusinessRuleException terminalEx =
        assertThrows(
            BusinessRuleException.class,
            () ->
                appointmentService.updateStatus(
                    "farmer_app@vetra.app",
                    createdApp.id(),
                    new UpdateAppointmentStatusRequest(
                        AppointmentStatus.CANCELLED, null, "Tried cancel")));
    assertEquals("APPT_005", terminalEx.getErrorCode());
  }

  @Test
  void testOptimisticLockingConflictHandling() {
    registerUsersAndAnimal();
    VetProfile vetProfile = vetProfileRepository.findAll().get(0);
    AnimalResponse animal =
        animalService.createAnimal(
            "farmer_app@vetra.app",
            new CreateAnimalRequest(
                "Opti",
                "TAG-OPT-1",
                "QR-OPT-1",
                Species.CATTLE,
                "Breed",
                AnimalGender.MALE,
                LocalDate.now().minusYears(1),
                null));

    AppointmentResponse appt =
        appointmentService.createAppointment(
            "farmer_app@vetra.app",
            new CreateAppointmentRequest(
                animal.id(),
                vetProfile.getId(),
                LocalDate.now().plusDays(1),
                LocalTime.of(14, 0),
                VisitType.GENERAL_CHECKUP,
                "Checkup"));

    Appointment entity1 = appointmentRepository.findById(appt.id()).orElseThrow();
    entity1.setVeterinarianNotes("First update");
    appointmentRepository.saveAndFlush(entity1);

    Appointment entity2 = new Appointment();
    entity2.setId(appt.id());
    entity2.setFarmer(entity1.getFarmer());
    entity2.setVeterinarian(entity1.getVeterinarian());
    entity2.setAnimal(entity1.getAnimal());
    entity2.setAppointmentDate(entity1.getAppointmentDate());
    entity2.setAppointmentTime(entity1.getAppointmentTime());
    entity2.setVisitType(entity1.getVisitType());
    entity2.setReason(entity1.getReason());
    entity2.setStatus(AppointmentStatus.CONFIRMED);
    entity2.setVersion(0L);

    assertThrows(
        ObjectOptimisticLockingFailureException.class,
        () -> {
          appointmentRepository.saveAndFlush(entity2);
        });
  }

  private void registerUsersAndAnimal() {
    authService.registerFarmer(
        new FarmerRegisterRequest(
            "farmer_app@vetra.app",
            "+1555099111",
            "pass123",
            "Farmer John",
            "Sunrise Farm",
            "Village",
            "District",
            "State",
            12.0,
            56.0,
            5));
    authService.registerVet(
        new VetRegisterRequest(
            "vet_app@vetra.app",
            "+1555099222",
            "pass123",
            "Dr. Sarah",
            "VET-REG-8899",
            "BVSc & AH",
            "Bovine Surgery",
            "City Vet Clinic",
            8,
            12.1,
            56.1));
  }
}
