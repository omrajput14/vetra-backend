package app.vetra.ai.service;

import static org.junit.jupiter.api.Assertions.*;

import app.vetra.ai.entity.AIAdvisorRiskLevel;
import app.vetra.ai.entity.AIAdvisorSessionStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AIAdvisorResponseMapperTest {

  private AIAdvisorResponseMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new AIAdvisorResponseMapper(new ObjectMapper());
  }

  @Test
  void parseAdvisorResponse_handlesMarkdownWrappedJson() {
    String raw =
        """
        ```json
        {
          "conversationState": "QUESTIONING",
          "replyMessage": "I have noted that. Is she still drinking water normally?",
          "followUpQuestions": [
            "What is her rectal temperature?",
            "Any visible nasal discharge?"
          ]
        }
        ```
        """;

    AIAdvisorResponseMapper.ParsedAdvisorOutput output = mapper.parseAdvisorResponse(raw, false);

    assertNotNull(output);
    assertEquals(AIAdvisorSessionStatus.QUESTIONING, output.status());
    assertEquals("I have noted that. Is she still drinking water normally?", output.replyMessage());
    assertEquals(2, output.followUpQuestions().size());
    assertEquals("What is her rectal temperature?", output.followUpQuestions().get(0));
    assertNull(output.assessment());
  }

  @Test
  void parseAdvisorResponse_handlesAssessmentWithMarkdown() {
    String raw =
        """
        ```json
        {
          "conversationState": "ASSESSMENT_GENERATED",
          "replyMessage": "Thank you for the complete timeline. Here is the preliminary screening assessment.",
          "followUpQuestions": [],
          "assessment": {
            "possibleConditions": [
              {
                "condition": "Acute Ruminal Acidosis (Suspected)",
                "confidence": 0.85,
                "reasoning": "Onset 1 day ago after feed ingestion with reduced appetite and lethargy."
              }
            ],
            "userReportedSymptoms": ["Sick for 1 day", "Decreased appetite"],
            "keyObservations": ["Acute gastrointestinal stasis pattern"],
            "riskLevel": "MODERATE",
            "requiresVeterinarianReview": true,
            "recommendedNextStep": "Provide fresh water and dry hay, monitor temperature twice daily.",
            "disclaimer": "This is an AI-assisted preliminary assessment."
          }
        }
        ```
        """;

    AIAdvisorResponseMapper.ParsedAdvisorOutput output = mapper.parseAdvisorResponse(raw, false);

    assertNotNull(output);
    assertEquals(AIAdvisorSessionStatus.ASSESSMENT_GENERATED, output.status());
    assertEquals(AIAdvisorRiskLevel.MODERATE, output.riskLevel());
    assertNotNull(output.assessment());
    assertEquals(1, output.assessment().possibleConditions().size());
    assertEquals("Acute Ruminal Acidosis (Suspected)", output.assessment().possibleConditions().get(0).condition());
    assertEquals(2, output.assessment().userReportedSymptoms().size());
  }

  @Test
  void parseAdvisorResponse_handlesEmergencyEscalation() {
    String raw =
        """
        ```json
        {
          "conversationState": "QUESTIONING",
          "replyMessage": "Checking symptoms..."
        }
        ```
        """;

    AIAdvisorResponseMapper.ParsedAdvisorOutput output = mapper.parseAdvisorResponse(raw, true);

    assertNotNull(output);
    assertEquals(AIAdvisorSessionStatus.URGENT_VETERINARY_REVIEW, output.status());
    assertEquals(AIAdvisorRiskLevel.CRITICAL, output.riskLevel());
  }

  @Test
  void parseAdvisorResponse_handlesMalformedJsonGracefully() {
    String malformed = "This is not valid json at all.";
    AIAdvisorResponseMapper.ParsedAdvisorOutput output = mapper.parseAdvisorResponse(malformed, false);

    assertNotNull(output);
    assertEquals(AIAdvisorSessionStatus.QUESTIONING, output.status());
    assertNotNull(output.replyMessage());
  }

  @Test
  void cleanMarkdown_handlesVariousFencesAndWhitespace() {
    assertEquals("{}", mapper.cleanMarkdown(null));
    assertEquals("{}", mapper.cleanMarkdown("   "));
    assertEquals("{\"key\":\"value\"}", mapper.cleanMarkdown("```json\n{\"key\":\"value\"}\n```"));
    assertEquals("{\"key\":\"value\"}", mapper.cleanMarkdown("```\n{\"key\":\"value\"}\n```"));
    assertEquals("{\"key\":\"value\"}", mapper.cleanMarkdown("Here is the result: ```json {\"key\":\"value\"} ``` Hope this helps."));
  }
}
