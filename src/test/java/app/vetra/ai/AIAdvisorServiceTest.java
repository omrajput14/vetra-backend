package app.vetra.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.ai.dto.advisor.AIAdvisorSessionResponse;
import app.vetra.ai.dto.advisor.AdvisorMessageRequest;
import app.vetra.ai.dto.advisor.CreateAdvisorSessionRequest;
import app.vetra.ai.entity.AIAdvisorRiskLevel;
import app.vetra.ai.entity.AIAdvisorSessionStatus;
import app.vetra.ai.service.AIAdvisorService;
import app.vetra.animal.dto.AnimalResponse;
import app.vetra.animal.dto.CreateAnimalRequest;
import app.vetra.animal.service.AnimalService;
import app.vetra.auth.dto.FarmerRegisterRequest;
import app.vetra.auth.service.AuthService;
import app.vetra.infrastructure.exception.BusinessRuleException;
import app.vetra.infrastructure.exception.UnauthorizedResourceAccessException;
import app.vetra.infrastructure.persistence.enums.AnimalGender;
import app.vetra.infrastructure.persistence.enums.Species;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for AIAdvisorService multi-turn conversational workflow and safety controls. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:vetra_ai_advisor_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
      "vetra.aws.s3.bucket-name=test-bucket",
      "vetra.ai.enabled=true",
      "vetra.ai.default-provider=noop"
    })
public class AIAdvisorServiceTest {

  @Autowired private AIAdvisorService advisorService;
  @Autowired private AuthService authService;
  @Autowired private AnimalService animalService;

  private String registerFarmer(String email, String phone) {
    var req =
        new FarmerRegisterRequest(
            email,
            phone,
            "Secret@12345",
            "Ramesh Farmer",
            "Village A",
            "Haveli",
            "Pune",
            "Maharashtra",
            18.5204,
            73.8567,
            5);
    authService.registerFarmer(req);
    return email;
  }

  private AnimalResponse createAnimal(String farmerEmail, String tagNumber) {
    var req =
        new CreateAnimalRequest(
            "Gauri",
            tagNumber,
            "QR-" + UUID.randomUUID(),
            Species.CATTLE,
            "Gir",
            AnimalGender.FEMALE,
            LocalDate.of(2022, 1, 1),
            "https://example.com/photo.jpg");
    return animalService.createAnimal(farmerEmail, req);
  }

  @Test
  void testCreateSessionAndMultiTurnConversation() {
    String farmerEmail = registerFarmer("farmer_adv_1@vetra.app", "+919876543201");
    AnimalResponse animal = createAnimal(farmerEmail, "TAG-ADV-001");

    // Turn 1: Initial message
    CreateAdvisorSessionRequest initReq =
        new CreateAdvisorSessionRequest("My cow has reduced feed intake and is slightly lethargic.");
    AIAdvisorSessionResponse turn1 =
        advisorService.createSession(farmerEmail, animal.id(), initReq);

    assertNotNull(turn1);
    assertNotNull(turn1.id());
    assertEquals(animal.id(), turn1.animalId());
    assertEquals(AIAdvisorSessionStatus.QUESTIONING, turn1.status());
    assertEquals(1, turn1.turnCount());
    assertFalse(turn1.messages().isEmpty());
    assertEquals(2, turn1.messages().size()); // 1 User, 1 AI

    // Turn 2: Follow-up answer -> Generates assessment
    AdvisorMessageRequest msgReq =
        new AdvisorMessageRequest(
            "She is drinking water but less than normal, mild nasal discharge, no fever noticed.");
    AIAdvisorSessionResponse turn2 =
        advisorService.sendMessage(farmerEmail, turn1.id(), msgReq);

    assertNotNull(turn2);
    assertEquals(AIAdvisorSessionStatus.ASSESSMENT_GENERATED, turn2.status());
    assertEquals(2, turn2.turnCount());
    assertEquals(AIAdvisorRiskLevel.MODERATE, turn2.riskLevel());
    assertTrue(turn2.requiresVetReview());
    assertNotNull(turn2.assessment());
    assertFalse(turn2.assessment().possibleConditions().isEmpty());
    assertTrue(turn2.assessment().recommendedNextStep().contains("veterinarian"));
    assertTrue(turn2.assessment().disclaimer().contains("AI-assisted preliminary assessment"));
  }

  @Test
  void testUnauthorizedAnimalAccessRejected() {
    String farmer1 = registerFarmer("farmer_adv_a@vetra.app", "+919876543202");
    String farmer2 = registerFarmer("farmer_adv_b@vetra.app", "+919876543203");
    AnimalResponse animal1 = createAnimal(farmer1, "TAG-ADV-002");

    CreateAdvisorSessionRequest req =
        new CreateAdvisorSessionRequest("Symptoms on another farmer's cow");

    assertThrows(
        UnauthorizedResourceAccessException.class,
        () -> advisorService.createSession(farmer2, animal1.id(), req));
  }

  @Test
  void testEmergencyKeywordTriggersUrgentEscalation() {
    String farmerEmail = registerFarmer("farmer_adv_emerg@vetra.app", "+919876543204");
    AnimalResponse animal = createAnimal(farmerEmail, "TAG-ADV-003");

    CreateAdvisorSessionRequest emergReq =
        new CreateAdvisorSessionRequest("Urgent! My cow collapsed and cannot stand at all!");
    AIAdvisorSessionResponse response =
        advisorService.createSession(farmerEmail, animal.id(), emergReq);

    assertNotNull(response);
    assertEquals(AIAdvisorSessionStatus.URGENT_VETERINARY_REVIEW, response.status());
    assertEquals(AIAdvisorRiskLevel.CRITICAL, response.riskLevel());
    assertTrue(response.requiresVetReview());
  }

  @Test
  void testSessionPaginationAndRetrieval() {
    String farmerEmail = registerFarmer("farmer_adv_page@vetra.app", "+919876543205");
    AnimalResponse animal = createAnimal(farmerEmail, "TAG-ADV-004");

    advisorService.createSession(farmerEmail, animal.id(), new CreateAdvisorSessionRequest("Session 1"));
    advisorService.createSession(farmerEmail, animal.id(), new CreateAdvisorSessionRequest("Session 2"));

    Page<AIAdvisorSessionResponse> page =
        advisorService.listSessionsForAnimal(farmerEmail, animal.id(), PageRequest.of(0, 10));

    assertNotNull(page);
    assertEquals(2, page.getTotalElements());
  }

  @Test
  void testFactualConsistencyAndSymptomDenial() {
    String farmerEmail = registerFarmer("farmer_adv_facts@vetra.app", "+919876543206");
    AnimalResponse animal = createAnimal(farmerEmail, "TAG-ADV-FACTS");

    // Turn 1: Initial report
    CreateAdvisorSessionRequest initReq =
        new CreateAdvisorSessionRequest("My cow has not eaten her morning grain.");
    AIAdvisorSessionResponse turn1 =
        advisorService.createSession(farmerEmail, animal.id(), initReq);
    assertEquals(AIAdvisorSessionStatus.QUESTIONING, turn1.status());

    // Turn 2: User explicitly reports normal water and denies bloat & coughing
    AdvisorMessageRequest msgReq =
        new AdvisorMessageRequest(
            "She is drinking water normally, temperature is 101.5 F, no bloat, and no coughing.");
    AIAdvisorSessionResponse turn2 =
        advisorService.sendMessage(farmerEmail, turn1.id(), msgReq);

    assertNotNull(turn2.assessment());
    assertEquals(AIAdvisorSessionStatus.ASSESSMENT_GENERATED, turn2.status());

    // Verify userReportedSymptoms preserves user facts and negations
    var userReports = turn2.assessment().userReportedSymptoms();
    assertNotNull(userReports);
    assertFalse(userReports.isEmpty());
    assertTrue(userReports.stream().anyMatch(s -> s.toLowerCase().contains("normal water")));
    assertTrue(userReports.stream().anyMatch(s -> s.toLowerCase().contains("absence of") && s.toLowerCase().contains("bloat")));
    assertTrue(userReports.stream().anyMatch(s -> s.toLowerCase().contains("absence of") && s.toLowerCase().contains("coughing")));

    // Verify keyObservations does NOT contradict user reports
    var observations = turn2.assessment().keyObservations();
    assertNotNull(observations);
    for (String obs : observations) {
      assertFalse(obs.toLowerCase().contains("decreased water intake"),
          "Assessment must not contradict user fact with 'decreased water intake'");
      assertFalse(obs.toLowerCase().contains("bloat present") || obs.toLowerCase().contains("coughing observed"),
          "Assessment must not report denied symptoms as present");
    }
  }

  @Test
  void testLanguageInstructionForwardingMarathiAndHindi() {
    String farmerEmail = registerFarmer("farmer_adv_lang@vetra.app", "+919876543207");
    AnimalResponse animal = createAnimal(farmerEmail, "TAG-ADV-LANG");

    // Start session in Marathi
    CreateAdvisorSessionRequest marathiReq =
        new CreateAdvisorSessionRequest("माझी गाय चारा खात नाहीये", "mr");
    AIAdvisorSessionResponse turn1 =
        advisorService.createSession(farmerEmail, animal.id(), marathiReq);

    assertNotNull(turn1);
    assertEquals(1, turn1.turnCount());

    // Send follow-up in Marathi
    AdvisorMessageRequest msgReq =
        new AdvisorMessageRequest("पाणी व्यवस्थित पीत आहे, ताप नाही", "mr");
    AIAdvisorSessionResponse turn2 =
        advisorService.sendMessage(farmerEmail, turn1.id(), msgReq);

    assertNotNull(turn2);
    assertNotNull(turn2.assessment());
    assertEquals(AIAdvisorSessionStatus.ASSESSMENT_GENERATED, turn2.status());
  }
}
