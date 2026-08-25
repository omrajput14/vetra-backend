package app.vetra.ai.gemini;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.vetra.ai.cache.CacheKeyGenerator;
import app.vetra.ai.config.AIGatewayProperties;
import app.vetra.ai.config.AIGatewayProperties.ModelConfig;
import app.vetra.ai.exception.AIProviderUnavailableException;
import app.vetra.ai.model.AICapability;
import app.vetra.ai.model.AIRequest;
import app.vetra.ai.prompt.PromptDescriptor;
import app.vetra.ai.provider.NoOpAIProvider;
import app.vetra.ai.provider.gemini.GeminiAIProvider;
import app.vetra.ai.provider.gemini.GeminiProperties;
import app.vetra.ai.registry.ModelRegistry;
import app.vetra.ai.registry.ProviderRegistry;
import app.vetra.ai.registry.ProviderRouter;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Rigorous tests validating the Gemini 2.5 Flash vision diagnostic pipeline:
 * routing, payload assembly, dynamic response extraction, failure fail-fast behavior,
 * and elimination of fake hardcoded diagnosis fallbacks.
 */
class GeminiVisionPipelineTest {

  private GeminiProperties geminiProperties;
  private GeminiAIProvider geminiProvider;
  private NoOpAIProvider noOpProvider;
  private CacheKeyGenerator cacheKeyGenerator;

  @BeforeEach
  void setUp() {
    geminiProperties = new GeminiProperties();
    geminiProperties.setEnabled(true);
    geminiProperties.setApiKey("test-gemini-key-12345");
    geminiProperties.setModel("gemini-2.5-flash");

    geminiProvider = new GeminiAIProvider(geminiProperties, WebClient.builder());
    noOpProvider = new NoOpAIProvider();
    cacheKeyGenerator = new CacheKeyGenerator();
  }

  private ModelConfig modelConfig(String provider, String modelId, List<String> caps, boolean enabled) {
    ModelConfig cfg = new ModelConfig();
    cfg.setProvider(provider);
    cfg.setModelId(modelId);
    cfg.setCapabilities(caps);
    cfg.setContextWindow(1048576);
    cfg.setMaxOutputTokens(8192);
    cfg.setEnabled(enabled);
    return cfg;
  }

  @Test
  @DisplayName("A & B: Gemini 2.5 Flash is selected for VISION diagnosis over NoOp when enabled")
  void testGeminiIsSelectedOverNoOpWhenAvailable() {
    Map<String, ModelConfig> models =
        Map.of(
            "diagnostics-fast",
            modelConfig("gemini", "gemini-2.5-flash", List.of("VISION", "JSON_MODE"), true),
            "noop-default",
            modelConfig("noop", "noop-v1", List.of("VISION", "JSON_MODE"), true));

    AIGatewayProperties.Builder propsBuilder =
        AIGatewayProperties.builder()
            .defaultProvider("gemini")
            .defaultModel("diagnostics-fast");
    models.forEach(propsBuilder::model);
    AIGatewayProperties gatewayProps = propsBuilder.build();

    ProviderRegistry providerRegistry =
        new ProviderRegistry(List.of(geminiProvider, noOpProvider), gatewayProps);
    ModelRegistry modelRegistry = new ModelRegistry(gatewayProps);
    ProviderRouter router = new ProviderRouter(providerRegistry, modelRegistry, gatewayProps);

    AIRequest request =
        new AIRequest(
            "diagnosis.visual.v1",
            Map.of(),
            "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
            false,
            Set.of(AICapability.VISION, AICapability.JSON_MODE),
            null);

    ProviderRouter.RoutingDecision decision = router.route(request);

    assertThat(decision.provider().providerName()).isEqualTo("gemini");
    assertThat(decision.model().modelId()).isEqualTo("gemini-2.5-flash");
    assertThat(decision.model().alias()).isEqualTo("diagnostics-fast");
  }

  @Test
  @DisplayName("C: Actual image Base64 data is converted to inlineData in Gemini request payload")
  @SuppressWarnings("unchecked")
  void testActualImageDataIncludedInGeminiPayload() throws Exception {
    String testBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
    String dataUri = "data:image/png;base64," + testBase64;

    AIRequest request =
        new AIRequest(
            "diagnosis.visual.v1",
            Map.of(),
            dataUri,
            false,
            Set.of(AICapability.VISION, AICapability.JSON_MODE),
            null);

    Method buildMethod =
        GeminiAIProvider.class.getDeclaredMethod("buildRequestPayload", AIRequest.class, String.class);
    buildMethod.setAccessible(true);

    Map<String, Object> payload =
        (Map<String, Object>) buildMethod.invoke(geminiProvider, request, "Diagnose this lesion.");

    assertThat(payload).containsKey("contents");
    List<Map<String, Object>> contents = (List<Map<String, Object>>) payload.get("contents");
    assertThat(contents).hasSize(1);

    List<Map<String, Object>> parts = (List<Map<String, Object>>) contents.get(0).get("parts");
    assertThat(parts).hasSize(2);

    Map<String, Object> imagePart = parts.get(0);
    assertThat(imagePart).containsKey("inlineData");

    Map<String, String> inlineData = (Map<String, String>) imagePart.get("inlineData");
    assertThat(inlineData.get("mimeType")).isEqualTo("image/png");
    assertThat(inlineData.get("data")).isEqualTo(testBase64);

    Map<String, Object> textPart = parts.get(1);
    assertThat(textPart.get("text")).isEqualTo("Diagnose this lesion.");
  }

  @Test
  @DisplayName("D: Different image payloads produce distinct SHA-256 cache keys")
  void testDifferentImagePayloadsProduceDistinctCacheKeys() {
    PromptDescriptor descriptor =
        new PromptDescriptor(
            "diagnosis.visual.v1",
            "v1",
            "Veterinary clinical diagnosis prompt",
            "Template {{animal}}",
            Set.of(AICapability.VISION, AICapability.JSON_MODE),
            "json",
            0.2,
            0.95,
            2048,
            true);

    AIRequest requestA =
        new AIRequest(
            "diagnosis.visual.v1",
            Map.of(),
            "data:image/jpeg;base64,AAA111222",
            false,
            Set.of(AICapability.VISION),
            null);

    AIRequest requestB =
        new AIRequest(
            "diagnosis.visual.v1",
            Map.of(),
            "data:image/jpeg;base64,ZZZ999888",
            false,
            Set.of(AICapability.VISION),
            null);

    String keyA = cacheKeyGenerator.generateKey(requestA, "Rendered Prompt", descriptor);
    String keyB = cacheKeyGenerator.generateKey(requestB, "Rendered Prompt", descriptor);

    assertThat(keyA).isNotEqualTo(keyB);
    assertThat(keyA).startsWith("vetra:ai:cache:");
    assertThat(keyB).startsWith("vetra:ai:cache:");
  }

  @Test
  @DisplayName("E & F: Dynamic response parsing uses real Gemini confidence and does not hardcode 0.88")
  void testDynamicResponseConfidenceParsing() throws Exception {
    String mockGeminiJsonResponse =
        """
        {
          "candidates": [
            {
              "content": {
                "parts": [
                  {
                    "text": "{\\"possibleCondition\\":\\"Lumpy Skin Disease\\",\\"confidence\\":0.94,\\"severity\\":\\"SEVERE\\",\\"observations\\":[\\"Generalized cutaneous nodules 2-5cm\\",\\"Edema in distal limbs\\"],\\"recommendedNextStep\\":\\"Immediate quarantine and supportive antipyretic therapy.\\",\\"requiresVeterinarianReview\\":true,\\"disclaimer\\":\\"Preliminary AI screening\\"}"
                  }
                ]
              },
              "finishReason": "STOP"
            }
          ],
          "usageMetadata": {
            "promptTokenCount": 350,
            "candidatesTokenCount": 120
          }
        }
        """;

    Method extractMethod =
        GeminiAIProvider.class.getDeclaredMethod("extractCandidateText", String.class);
    extractMethod.setAccessible(true);
    String candidateText = (String) extractMethod.invoke(geminiProvider, mockGeminiJsonResponse);

    assertThat(candidateText).contains("Lumpy Skin Disease");
    assertThat(candidateText).contains("0.94");
    assertThat(candidateText).doesNotContain("0.88");
    assertThat(candidateText).doesNotContain("Bovine Dermatophilosis");
  }

  @Test
  @DisplayName("G: NoOp stub is sanitized and no longer returns fake Dermatophilosis or 0.88")
  void testNoOpProviderSanitized() {
    AIRequest request =
        new AIRequest(
            "diagnosis.visual.v1",
            Map.of(),
            "data:image/jpeg;base64,test",
            false,
            Set.of(AICapability.VISION),
            null);

    var response = noOpProvider.execute(request, "Diagnose image");

    assertThat(response.content()).doesNotContain("Bovine Dermatophilosis (Suspected)");
    assertThat(response.content()).doesNotContain("0.88");
    assertThat(response.content()).contains("Simulated Assessment (NoOp Development Stub)");
    assertThat(response.content()).contains("0.00");
  }

  @Test
  @DisplayName("H: Missing Gemini credentials makes Gemini unavailable and throws on execution")
  void testMissingCredentialsFailsFast() {
    GeminiProperties noKeyProps = new GeminiProperties();
    noKeyProps.setEnabled(true);
    noKeyProps.setApiKey(""); // Missing key
    GeminiAIProvider unconfiguredProvider = new GeminiAIProvider(noKeyProps, WebClient.builder());

    assertThat(unconfiguredProvider.isAvailable()).isFalse();

    AIRequest request =
        new AIRequest("diagnosis.visual.v1", Map.of(), "http://img.jpg", false, Set.of(), null);
    assertThatThrownBy(() -> unconfiguredProvider.execute(request, "prompt"))
        .isInstanceOf(AIProviderUnavailableException.class)
        .hasMessageContaining("missing API key");
  }
}
