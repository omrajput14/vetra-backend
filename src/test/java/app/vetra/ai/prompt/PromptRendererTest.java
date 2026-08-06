package app.vetra.ai.prompt;

import static org.junit.jupiter.api.Assertions.*;

import app.vetra.ai.exception.AIConfigurationException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PromptRendererTest {

  private PromptRenderer renderer;

  @BeforeEach
  void setUp() {
    renderer = new PromptRenderer();
  }

  @Test
  void testRender_successfulReplacement() {
    String template = "Analyze {{species}} age {{age}}.";
    Map<String, Object> context = Map.of("species", "Bovine", "age", 5);

    String result = renderer.render(template, context);
    assertEquals("Analyze Bovine age 5.", result);
  }

  @Test
  void testRender_missingVariables_throwsException() {
    String template = "Analyze {{species}} age {{age}}.";
    Map<String, Object> context = Map.of("species", "Bovine");

    AIConfigurationException ex =
        assertThrows(AIConfigurationException.class, () -> renderer.render(template, context));
    assertTrue(ex.getMessage().contains("age"), "Should indicate missing 'age' variable");
    assertEquals("AI_PROMPT_MISSING_VARS", ex.getErrorCode());
  }

  @Test
  void testRender_noVariables() {
    String template = "Standard prompt.";
    Map<String, Object> context = Map.of("unused", "value");

    String result = renderer.render(template, context);
    assertEquals("Standard prompt.", result);
  }

  @Test
  void testRender_nullTemplate() {
    assertNull(renderer.render(null, Map.of()));
  }
}
