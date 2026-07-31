package app.vetra.ai.gemini;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.ai.provider.gemini.GeminiPromptBuilder;
import org.junit.jupiter.api.Test;

class GeminiPromptBuilderTest {

  private final GeminiPromptBuilder promptBuilder = new GeminiPromptBuilder();

  @Test
  void testBuildPromptGeneratesStructuredInstructions() {
    String prompt = promptBuilder.buildPrompt();

    assertNotNull(prompt);
    assertTrue(prompt.contains("requiresVeterinarianReview"));
    assertTrue(prompt.contains("condition"));
    assertTrue(prompt.contains("confidence"));
  }
}
