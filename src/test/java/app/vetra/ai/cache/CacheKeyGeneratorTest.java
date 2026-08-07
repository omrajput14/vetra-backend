package app.vetra.ai.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.ai.entity.AIProviderType;
import app.vetra.ai.model.AICapability;
import app.vetra.ai.model.AIRequest;
import app.vetra.ai.prompt.PromptDescriptor;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CacheKeyGeneratorTest {

  private CacheKeyGenerator generator;
  private PromptDescriptor descriptorV1;
  private PromptDescriptor descriptorV2;

  @BeforeEach
  void setUp() {
    generator = new CacheKeyGenerator();
    descriptorV1 =
        new PromptDescriptor(
            "diagnosis.visual.v1",
            "1.0.0",
            "Visual diagnosis",
            "Analyze {{symptom}}",
            Set.of(AICapability.VISION),
            "text",
            0.5,
            0.9,
            100,
            true);

    descriptorV2 =
        new PromptDescriptor(
            "diagnosis.visual.v1",
            "2.0.0", // Prompt version change
            "Visual diagnosis v2",
            "Analyze {{symptom}}",
            Set.of(AICapability.VISION),
            "text",
            0.5,
            0.9,
            100,
            true);
  }

  @Test
  void testKeyGenerationIsDeterministic() {
    AIRequest request1 =
        new AIRequest("diagnosis.visual.v1", Map.of("symptom", "cough"), null, false, Set.of(), null);
    AIRequest request2 =
        new AIRequest("diagnosis.visual.v1", Map.of("symptom", "cough"), null, false, Set.of(), null);

    String key1 = generator.generateKey(request1, "Analyze cough", descriptorV1);
    String key2 = generator.generateKey(request2, "Analyze cough", descriptorV1);

    assertNotNull(key1);
    assertTrue(key1.startsWith("vetra:ai:cache:"));
    assertEquals(key1, key2);
  }

  @Test
  void testPromptVersionInvalidatesCacheKey() {
    AIRequest request =
        new AIRequest("diagnosis.visual.v1", Map.of("symptom", "cough"), null, false, Set.of(), null);

    String keyV1 = generator.generateKey(request, "Analyze cough", descriptorV1);
    String keyV2 = generator.generateKey(request, "Analyze cough", descriptorV2);

    assertNotEquals(keyV1, keyV2);
  }

  @Test
  void testRenderedTextChangeInvalidatesCacheKey() {
    AIRequest request =
        new AIRequest("diagnosis.visual.v1", Map.of("symptom", "fever"), null, false, Set.of(), null);

    String keyCough = generator.generateKey(request, "Analyze cough", descriptorV1);
    String keyFever = generator.generateKey(request, "Analyze fever", descriptorV1);

    assertNotEquals(keyCough, keyFever);
  }

  @Test
  void testRequestedProviderAffectsCacheKey() {
    AIRequest reqAny =
        new AIRequest("diagnosis.visual.v1", Map.of(), null, false, Set.of(), null);
    AIRequest reqGemini =
        new AIRequest("diagnosis.visual.v1", Map.of(), null, false, Set.of(), AIProviderType.GEMINI);

    String keyAny = generator.generateKey(reqAny, "Analyze", descriptorV1);
    String keyGemini = generator.generateKey(reqGemini, "Analyze", descriptorV1);

    assertNotEquals(keyAny, keyGemini);
  }

  @Test
  void testHashString() {
    String hash1 = generator.hashString("hello");
    String hash2 = generator.hashString("hello");
    assertEquals(hash1, hash2);
    assertEquals(64, hash1.length()); // SHA-256 hex length
  }
}
