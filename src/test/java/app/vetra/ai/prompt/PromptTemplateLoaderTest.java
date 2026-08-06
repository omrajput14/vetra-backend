package app.vetra.ai.prompt;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class PromptTemplateLoaderTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void testLoadPrompts_discoversDiagnosisVisualV1() {
    PromptTemplateLoader loader = new PromptTemplateLoader(objectMapper);
    List<PromptDescriptor> prompts = loader.loadPrompts();

    assertFalse(prompts.isEmpty(), "Should load at least one prompt");

    PromptDescriptor descriptor =
        prompts.stream()
            .filter(p -> "diagnosis.visual.v1".equals(p.promptId()))
            .findFirst()
            .orElse(null);

    assertNotNull(descriptor, "diagnosis.visual.v1 should be discovered");
    assertEquals("v1", descriptor.version());
    assertEquals(0.2, descriptor.temperature());
    assertEquals("json", descriptor.expectedFormat());
    assertTrue(descriptor.enabled());
    assertNotNull(descriptor.template());
  }
}
