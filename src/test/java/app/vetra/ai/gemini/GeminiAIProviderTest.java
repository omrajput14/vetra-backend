package app.vetra.ai.gemini;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.ai.exception.AIProviderUnavailableException;
import app.vetra.ai.model.AIRequest;
import app.vetra.ai.provider.gemini.GeminiAIProvider;
import app.vetra.ai.provider.gemini.GeminiProperties;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class GeminiAIProviderTest {

  private GeminiProperties properties;
  private GeminiAIProvider provider;

  @BeforeEach
  void setUp() {
    properties = new GeminiProperties();
    properties.setEnabled(false);
    properties.setApiKey("test-dummy-api-key");
    properties.setModel("gemini-1.5-flash");

    provider = new GeminiAIProvider(properties, WebClient.builder());
  }

  @Test
  void testProviderAvailabilityWhenDisabled() {
    properties.setEnabled(false);
    assertFalse(provider.isAvailable());

    properties.setEnabled(true);
    properties.setApiKey("");
    assertFalse(provider.isAvailable());

    properties.setApiKey("valid-key");
    assertTrue(provider.isAvailable());
  }

  @Test
  void testExecuteThrowsAIProviderUnavailableWhenDisabled() {
    properties.setEnabled(false);
    AIRequest request = new AIRequest("promptId", Map.of(), "https://example.com/image.jpg", false, Set.of(), null);
    assertThrows(
        AIProviderUnavailableException.class,
        () -> provider.execute(request, "Analyze image"));
  }

  @Test
  void testProviderMetadata() {
    assertEquals("GEMINI", provider.providerName());
  }
}
