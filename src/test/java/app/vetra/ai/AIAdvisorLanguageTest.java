package app.vetra.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import app.vetra.infrastructure.persistence.enums.AnimalGender;
import app.vetra.infrastructure.persistence.enums.Species;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Targeted multilingual test suite for AI Veterinary Advisor verifying localized responses in
 * Marathi, Hindi, and English with strict clinical safety and schema invariant preservation.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:vetra_ai_advisor_lang_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
public class AIAdvisorLanguageTest {

  @Autowired private AIAdvisorService advisorService;
  @Autowired private AuthService authService;
  @Autowired private AnimalService animalService;

  private String registerFarmer(String email, String phone, String language) {
    var req =
        new FarmerRegisterRequest(
            email,
            phone,
            "Secret@12345",
            "Farmer Test",
            "Farm Test",
            "Village Test",
            "Haveli",
            "Maharashtra",
            18.5204,
            73.8567,
            5,
            language);
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
  void testMarathiAdvisorLanguageBehavior() {
    String farmerEmail = registerFarmer("farmer_mr@vetra.app", "+919876543101", "mr");
    AnimalResponse animal = createAnimal(farmerEmail, "TAG-MR-001");

    // Turn 1 in Marathi
    CreateAdvisorSessionRequest req1 =
        new CreateAdvisorSessionRequest("माझी गाय चारा खात नाहीये आणि सुस्त वाटते आहे", "mr");
    AIAdvisorSessionResponse turn1 = advisorService.createSession(farmerEmail, animal.id(), req1);

    assertNotNull(turn1);
    assertEquals(AIAdvisorSessionStatus.QUESTIONING, turn1.status());
    assertEquals(1, turn1.turnCount());
    assertFalse(turn1.messages().isEmpty());

    var advisorMsg1 = turn1.messages().get(1);
    assertNotNull(advisorMsg1.content());
    assertTrue(
        advisorMsg1.content().contains("समस्येबद्दल") || advisorMsg1.content().contains("लक्षणे"),
        "Turn 1 reply message must be in Marathi");
    assertFalse(advisorMsg1.followUpQuestions().isEmpty());
    assertTrue(
        advisorMsg1.followUpQuestions().get(0).contains("का?"),
        "Follow-up questions must be in Marathi");

    // Turn 2 in Marathi
    AdvisorMessageRequest req2 =
        new AdvisorMessageRequest("पाणी व्यवस्थित पीत आहे, पण थोडा ताप वाटतो", "mr");
    AIAdvisorSessionResponse turn2 = advisorService.sendMessage(farmerEmail, turn1.id(), req2);

    assertNotNull(turn2);
    assertEquals(AIAdvisorSessionStatus.ASSESSMENT_GENERATED, turn2.status());
    assertEquals(AIAdvisorRiskLevel.MODERATE, turn2.riskLevel());
    assertTrue(turn2.requiresVetReview());

    assertNotNull(turn2.assessment());
    assertTrue(
        turn2.assessment().disclaimer().contains("पशुवैद्यक")
            || turn2.assessment().disclaimer().contains("प्राथमिक मूल्यांकन"),
        "Disclaimer must be in Marathi");
    assertTrue(
        turn2.assessment().recommendedNextStep().contains("स्वच्छ")
            || turn2.assessment().recommendedNextStep().contains("पशुवैद्य"),
        "Recommended next step must be in Marathi");
  }

  @Test
  void testHindiAdvisorLanguageBehavior() {
    String farmerEmail = registerFarmer("farmer_hi@vetra.app", "+919876543102", "hi");
    AnimalResponse animal = createAnimal(farmerEmail, "TAG-HI-001");

    // Turn 1 in Hindi
    CreateAdvisorSessionRequest req1 =
        new CreateAdvisorSessionRequest("मेरी गाय खाना नहीं खा रही है और सुस्त लग रही है", "hi");
    AIAdvisorSessionResponse turn1 = advisorService.createSession(farmerEmail, animal.id(), req1);

    assertNotNull(turn1);
    assertEquals(AIAdvisorSessionStatus.QUESTIONING, turn1.status());
    assertEquals(1, turn1.turnCount());

    var advisorMsg1 = turn1.messages().get(1);
    assertNotNull(advisorMsg1.content());
    assertTrue(
        advisorMsg1.content().contains("चिंता") || advisorMsg1.content().contains("लक्षण"),
        "Turn 1 reply message must be in Hindi");
    assertFalse(advisorMsg1.followUpQuestions().isEmpty());
    assertTrue(
        advisorMsg1.followUpQuestions().get(0).contains("क्या"),
        "Follow-up questions must be in Hindi");

    // Turn 2 in Hindi
    AdvisorMessageRequest req2 =
        new AdvisorMessageRequest("पानी पी रही है, कोई बुखार नहीं है", "hi");
    AIAdvisorSessionResponse turn2 = advisorService.sendMessage(farmerEmail, turn1.id(), req2);

    assertNotNull(turn2);
    assertEquals(AIAdvisorSessionStatus.ASSESSMENT_GENERATED, turn2.status());
    assertEquals(AIAdvisorRiskLevel.MODERATE, turn2.riskLevel());

    assertNotNull(turn2.assessment());
    assertTrue(
        turn2.assessment().disclaimer().contains("पशुचिकित्सक")
            || turn2.assessment().disclaimer().contains("प्रारंभिक मूल्यांकन"),
        "Disclaimer must be in Hindi");
    assertTrue(
        turn2.assessment().recommendedNextStep().contains("पशु")
            || turn2.assessment().recommendedNextStep().contains("पशुचिकित्सक"),
        "Recommended next step must be in Hindi");
  }

  @Test
  void testEnglishAdvisorLanguageBehavior() {
    String farmerEmail = registerFarmer("farmer_en@vetra.app", "+919876543103", "en");
    AnimalResponse animal = createAnimal(farmerEmail, "TAG-EN-001");

    // Turn 1 in English
    CreateAdvisorSessionRequest req1 =
        new CreateAdvisorSessionRequest("My cow is not eating well today", "en");
    AIAdvisorSessionResponse turn1 = advisorService.createSession(farmerEmail, animal.id(), req1);

    assertNotNull(turn1);
    assertEquals(AIAdvisorSessionStatus.QUESTIONING, turn1.status());

    var advisorMsg1 = turn1.messages().get(1);
    assertTrue(advisorMsg1.content().contains("understand your concern"));

    // Turn 2 in English
    AdvisorMessageRequest req2 =
        new AdvisorMessageRequest("She is drinking water normally, no fever", "en");
    AIAdvisorSessionResponse turn2 = advisorService.sendMessage(farmerEmail, turn1.id(), req2);

    assertNotNull(turn2);
    assertEquals(AIAdvisorSessionStatus.ASSESSMENT_GENERATED, turn2.status());
    assertTrue(turn2.assessment().disclaimer().contains("AI-assisted preliminary assessment"));
  }
}
