package app.vetra.ai.gemini;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.ai.entity.AIProviderType;
import app.vetra.ai.exception.AIProviderUnavailableException;
import app.vetra.ai.provider.gemini.GeminiAIProvider;
import app.vetra.ai.provider.gemini.GeminiPromptBuilder;
import app.vetra.ai.provider.gemini.GeminiProperties;
import app.vetra.ai.provider.gemini.GeminiResponseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class GeminiAIProviderTest {

  private GeminiProperties properties;
  private GeminiPromptBuilder promptBuilder;
  private GeminiResponseMapper responseMapper;
  private GeminiAIProvider provider;

  @BeforeEach
  void setUp() {
    properties = new GeminiProperties();
    properties.setEnabled(false);
    properties.setApiKey("test-dummy-api-key");
    properties.setModel("gemini-1.5-flash");

    promptBuilder = new GeminiPromptBuilder();
    responseMapper = new GeminiResponseMapper();

    provider = new GeminiAIProvider(properties, promptBuilder, responseMapper, WebClient.builder());
  }

  @Test
  void testSupportsGeminiType() {
    assertTrue(provider.supports(AIProviderType.GEMINI));
    assertFalse(provider.supports(AIProviderType.NONE));
    assertFalse(provider.supports(AIProviderType.OPENAI));
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
  void testAnalyzeRejectsInvalidImageUrls() {
    properties.setEnabled(true);
    properties.setApiKey("valid-key");

    assertThrows(
        IllegalArgumentException.class, () -> provider.analyze("invalid-text-without-extension"));
  }

  @Test
  void testAnalyzeThrowsAIProviderUnavailableWhenDisabled() {
    properties.setEnabled(false);
    assertThrows(
        AIProviderUnavailableException.class,
        () -> provider.analyze("https://s3.amazonaws.com/vetra/cow.jpeg"));
  }

  @Test
  void testProviderMetadata() {
    assertEquals("GEMINI", provider.providerName());
    assertEquals(AIProviderType.GEMINI, provider.providerType());
    assertEquals("gemini-1.5-flash", provider.model());
  }
}
