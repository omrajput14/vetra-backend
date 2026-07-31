package app.vetra.ai.gemini;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.ai.entity.AIProviderType;
import app.vetra.ai.exception.AIInferenceException;
import app.vetra.ai.provider.AIInferenceResult;
import app.vetra.ai.provider.gemini.GeminiResponseMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class GeminiResponseMapperTest {

  private final GeminiResponseMapper mapper = new GeminiResponseMapper();

  @Test
  void testMapToInferenceResultParsesCleanJson() {
    String jsonResponse = """
        {
          "condition": "Bovine Foot and Mouth Disease (FMD)",
          "confidence": 0.92,
          "observations": ["Lesions on tongue", "Interdigital blister"],
          "recommendations": ["Isolate animal immediately", "Notify district vet officer"],
          "requiresVeterinarianReview": true
        }
        """;

    AIInferenceResult result = mapper.mapToInferenceResult(jsonResponse, "gemini-1.5-flash", "REQ-1234", 250L);

    assertNotNull(result);
    assertEquals(AIProviderType.GEMINI, result.provider());
    assertEquals("gemini-1.5-flash", result.model());
    assertEquals(new BigDecimal("0.92"), result.confidence());
    assertTrue(result.diagnosis().contains("Bovine Foot and Mouth Disease"));
    assertTrue(result.diagnosis().contains("Lesions on tongue"));
  }

  @Test
  void testMapToInferenceResultCleansMarkdownJsonBlocks() {
    String markdownResponse = """
        ```json
        {
          "condition": "Bovine Dermatitis",
          "confidence": 0.80,
          "observations": ["Skin redness"],
          "recommendations": ["Apply topical antiseptic"],
          "requiresVeterinarianReview": true
        }
        ```
        """;

    AIInferenceResult result = mapper.mapToInferenceResult(markdownResponse, "gemini-1.5-flash", "REQ-5678", 180L);

    assertNotNull(result);
    assertEquals(AIProviderType.GEMINI, result.provider());
    assertTrue(result.diagnosis().contains("Bovine Dermatitis"));
  }

  @Test
  void testMapToInferenceResultThrowsAIInferenceExceptionOnMalformedJson() {
    String malformedJson = "{ invalid json body }";

    assertThrows(AIInferenceException.class, () ->
        mapper.mapToInferenceResult(malformedJson, "gemini-1.5-flash", "REQ-0000", 100L));
  }
}
