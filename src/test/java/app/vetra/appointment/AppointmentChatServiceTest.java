package app.vetra.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.vetra.animal.dto.AnimalResponse;
import app.vetra.animal.dto.CreateAnimalRequest;
import app.vetra.animal.service.AnimalService;
import app.vetra.appointment.dto.ChatMessageDto;
import app.vetra.appointment.dto.CreateAppointmentRequest;
import app.vetra.appointment.dto.SendChatMessageRequest;
import app.vetra.appointment.service.AppointmentChatService;
import app.vetra.appointment.service.AppointmentService;
import app.vetra.auth.dto.FarmerRegisterRequest;
import app.vetra.auth.dto.VetRegisterRequest;
import app.vetra.auth.repository.VetProfileRepository;
import app.vetra.auth.service.AuthService;
import app.vetra.infrastructure.exception.UnauthorizedResourceAccessException;
import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.infrastructure.persistence.enums.AnimalGender;
import app.vetra.infrastructure.persistence.enums.Species;
import app.vetra.infrastructure.persistence.enums.VisitType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for direct consultation chat and structured veterinarian Rx instructions. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:vetra_chat_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
      "vetra.aws.s3.bucket=vetra-uploads-test",
      "vetra.aws.access-key=test-access-key",
      "vetra.aws.secret-key=test-secret-key"
    })
class AppointmentChatServiceTest {

  @Autowired private AppointmentChatService appointmentChatService;
  @Autowired private AppointmentService appointmentService;
  @Autowired private AuthService authService;
  @Autowired private AnimalService animalService;
  @Autowired private VetProfileRepository vetProfileRepository;

  @Test
  void testConsultationChatAndVeterinarianTreatmentInstructionFlow() {
    String farmerEmail = "farmer.chat." + System.currentTimeMillis() + "@test.com";
    String vetEmail = "dr.chat." + System.currentTimeMillis() + "@test.com";
    String thirdPartyEmail = "intruder." + System.currentTimeMillis() + "@test.com";

    authService.registerFarmer(
        new FarmerRegisterRequest(
            farmerEmail,
            "+919876549001",
            "Password123!",
            "Ramesh Patil",
            "Patil Farm",
            "Kothrud",
            "Pune",
            "MH",
            18.5,
            73.8,
            5));

    authService.registerVet(
        new VetRegisterRequest(
            vetEmail,
            "+919876549002",
            "Password123!",
            "Dr. Ananya Roy",
            "REG-" + System.currentTimeMillis(),
            "BVSc & AH",
            "Bovine Medicine",
            "Roy Animal Hospital",
            8,
            18.51,
            73.81));

    authService.registerFarmer(
        new FarmerRegisterRequest(
            thirdPartyEmail,
            "+919876549003",
            "Password123!",
            "Third Party",
            "Other Farm",
            "Kothrud",
            "Pune",
            "MH",
            18.5,
            73.8,
            2));

    AnimalResponse animal =
        animalService.createAnimal(
            farmerEmail,
            new CreateAnimalRequest(
                "Gauri",
                "TAG-" + System.currentTimeMillis(),
                "QR-" + System.currentTimeMillis(),
                Species.CATTLE,
                "Gir",
                AnimalGender.FEMALE,
                LocalDate.now().minusYears(3),
                null));

    List<VetProfile> vets = vetProfileRepository.findAll();
    VetProfile vet = vets.stream().filter(v -> v.getUser().getEmail().equals(vetEmail)).findFirst().orElseThrow();

    var appt =
        appointmentService.createAppointment(
            farmerEmail,
            new CreateAppointmentRequest(
                animal.id(),
                vet.getId(),
                LocalDate.now().plusDays(2),
                LocalTime.of(10, 0),
                VisitType.GENERAL_CHECKUP,
                "Reduced milk yield and mild lethargy"));

    // 1. Farmer sends message to Veterinarian
    ChatMessageDto farmerMsg =
        appointmentChatService.sendMessage(
            farmerEmail,
            appt.id(),
            new SendChatMessageRequest("Doctor, the cow has not been eating well since morning.", "TEXT", null));

    assertNotNull(farmerMsg);
    assertEquals("TEXT", farmerMsg.messageType());
    assertEquals("Doctor, the cow has not been eating well since morning.", farmerMsg.content());

    // 2. Veterinarian sends structured treatment instruction
    String rxPayload =
        "{\"diagnosis\":\"Rumen Atony\",\"medication\":\"Rumenotoric bolus\",\"dosage\":\"1 bolus BID for 3 days\",\"careInstructions\":\"Provide green fodder and fresh water\"}";
    ChatMessageDto vetRx =
        appointmentChatService.sendMessage(
            vetEmail,
            appt.id(),
            new SendChatMessageRequest(
                "Prescribed rumenotoric therapy and dietary adjustments",
                "TREATMENT_INSTRUCTION",
                rxPayload));

    assertNotNull(vetRx);
    assertEquals("TREATMENT_INSTRUCTION", vetRx.messageType());
    assertNotNull(vetRx.treatmentPayloadJson());

    // 3. Farmer attempts to author treatment instruction -> Forbidden
    assertThrows(
        UnauthorizedResourceAccessException.class,
        () ->
            appointmentChatService.sendMessage(
                farmerEmail,
                appt.id(),
                new SendChatMessageRequest("Self treatment", "TREATMENT_INSTRUCTION", null)));

    // 4. Retrieve conversation messages
    List<ChatMessageDto> messages = appointmentChatService.getMessages(farmerEmail, appt.id());
    assertEquals(2, messages.size());

    // 5. Unauthorized 3rd party access forbidden
    assertThrows(
        UnauthorizedResourceAccessException.class,
        () -> appointmentChatService.getMessages(thirdPartyEmail, appt.id()));
  }
}
