package app.vetra.ai.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.vetra.ai.config.AIGatewayProperties;
import app.vetra.ai.config.AIGatewayProperties.ModelConfig;
import app.vetra.ai.exception.AIProviderUnavailableException;
import app.vetra.ai.model.AICapability;
import app.vetra.ai.model.AIRequest;
import app.vetra.ai.provider.AIProvider;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ProviderRouter}: default routing, capability-based routing, and failure
 * cases when no suitable provider exists.
 */
class ProviderRouterTest {

  private AIProvider noopProvider;
  private AIProvider visionProvider;

  @BeforeEach
  void setUp() {
    noopProvider = mock(AIProvider.class);
    when(noopProvider.providerName()).thenReturn("noop");
    when(noopProvider.isAvailable()).thenReturn(true);
    when(noopProvider.supportedCapabilities()).thenReturn(Set.of());

    visionProvider = mock(AIProvider.class);
    when(visionProvider.providerName()).thenReturn("vision-provider");
    when(visionProvider.isAvailable()).thenReturn(true);
    when(visionProvider.supportedCapabilities())
        .thenReturn(Set.of(AICapability.VISION, AICapability.JSON_MODE));
  }

  private AIGatewayProperties buildProperties(
      String defaultProvider, String defaultModel, Map<String, ModelConfig> models) {
    AIGatewayProperties.Builder builder =
        AIGatewayProperties.builder().defaultProvider(defaultProvider).defaultModel(defaultModel);
    models.forEach(builder::model);
    return builder.build();
  }

  private ModelConfig modelFor(String provider, List<String> caps) {
    ModelConfig cfg = new ModelConfig();
    cfg.setProvider(provider);
    cfg.setModelId("test-model");
    cfg.setContextWindow(4096);
    cfg.setMaxOutputTokens(1024);
    cfg.setCapabilities(caps);
    cfg.setEnabled(true);
    return cfg;
  }

  @Test
  @DisplayName("route() with no capabilities returns default provider and model")
  void testRoute_noCapabilities_returnsDefault() {
    Map<String, ModelConfig> models = Map.of("noop-default", modelFor("noop", List.of()));
    AIGatewayProperties props = buildProperties("noop", "noop-default", models);

    ProviderRegistry providerRegistry = new ProviderRegistry(List.of(noopProvider), props);
    ModelRegistry modelRegistry = new ModelRegistry(props);
    ProviderRouter router = new ProviderRouter(providerRegistry, modelRegistry, props);

    AIRequest request = new AIRequest("test-prompt", Map.of(), null, false, Set.of());
    ProviderRouter.RoutingDecision decision = router.route(request);

    assertThat(decision.provider().providerName()).isEqualTo("noop");
    assertThat(decision.model().alias()).isEqualTo("noop-default");
  }

  @Test
  @DisplayName("route() with VISION capability selects vision-capable provider")
  void testRoute_withVision_selectsVisionProvider() {
    Map<String, ModelConfig> models =
        Map.of(
            "noop-default", modelFor("noop", List.of()),
            "vision-model", modelFor("vision-provider", List.of("VISION", "JSON_MODE")));
    AIGatewayProperties props = buildProperties("noop", "noop-default", models);

    ProviderRegistry providerRegistry =
        new ProviderRegistry(List.of(noopProvider, visionProvider), props);
    ModelRegistry modelRegistry = new ModelRegistry(props);
    ProviderRouter router = new ProviderRouter(providerRegistry, modelRegistry, props);

    AIRequest request =
        new AIRequest(
            "diag-prompt", Map.of(), "http://img.jpg", false, Set.of(AICapability.VISION));
    ProviderRouter.RoutingDecision decision = router.route(request);

    assertThat(decision.provider().providerName()).isEqualTo("vision-provider");
    assertThat(decision.model().alias()).isEqualTo("vision-model");
  }

  @Test
  @DisplayName("route() throws AIProviderUnavailableException when no provider meets capabilities")
  void testRoute_noCapableProvider_throws() {
    Map<String, ModelConfig> models = Map.of("noop-default", modelFor("noop", List.of()));
    AIGatewayProperties props = buildProperties("noop", "noop-default", models);

    ProviderRegistry providerRegistry = new ProviderRegistry(List.of(noopProvider), props);
    ModelRegistry modelRegistry = new ModelRegistry(props);
    ProviderRouter router = new ProviderRouter(providerRegistry, modelRegistry, props);

    AIRequest request =
        new AIRequest("diag", Map.of(), null, false, Set.of(AICapability.STREAMING));

    assertThatThrownBy(() -> router.route(request))
        .isInstanceOf(AIProviderUnavailableException.class)
        .hasMessageContaining("STREAMING");
  }

  @Test
  @DisplayName("resolveDefault() prefers default model when it is capability-capable")
  void testResolveByCapabilities_prefersDefault_whenCapable() {
    // Default model is "noop-default" but it does not support VISION
    // Only vision-model supports VISION — expect vision-model to be selected
    Map<String, ModelConfig> models =
        Map.of(
            "noop-default", modelFor("noop", List.of()),
            "vision-model", modelFor("vision-provider", List.of("VISION")));
    AIGatewayProperties props = buildProperties("vision-provider", "vision-model", models);

    ProviderRegistry providerRegistry =
        new ProviderRegistry(List.of(noopProvider, visionProvider), props);
    ModelRegistry modelRegistry = new ModelRegistry(props);
    ProviderRouter router = new ProviderRouter(providerRegistry, modelRegistry, props);

    // vision-model is the default, and it matches — should be preferred
    AIRequest request = new AIRequest("p", Map.of(), null, false, Set.of(AICapability.VISION));
    ProviderRouter.RoutingDecision decision = router.route(request);

    assertThat(decision.model().alias()).isEqualTo("vision-model");
  }
}
