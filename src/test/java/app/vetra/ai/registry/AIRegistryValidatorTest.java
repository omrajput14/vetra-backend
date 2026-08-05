package app.vetra.ai.registry;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.vetra.ai.config.AIGatewayProperties;
import app.vetra.ai.config.AIGatewayProperties.ModelConfig;
import app.vetra.ai.exception.AIConfigurationException;
import app.vetra.ai.provider.AIProvider;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AIRegistryValidator}: startup validation covering missing defaults, unknown
 * provider references, and a clean valid configuration.
 */
class AIRegistryValidatorTest {

  private AIProvider mockProvider(String name) {
    AIProvider p = mock(AIProvider.class);
    when(p.providerName()).thenReturn(name);
    when(p.isAvailable()).thenReturn(true);
    when(p.supportedCapabilities()).thenReturn(Set.of());
    return p;
  }

  private ModelConfig modelFor(String provider) {
    ModelConfig cfg = new ModelConfig();
    cfg.setProvider(provider);
    cfg.setModelId("test-model");
    cfg.setContextWindow(1024);
    cfg.setMaxOutputTokens(256);
    cfg.setEnabled(true);
    return cfg;
  }

  @Test
  @DisplayName("Valid configuration passes validation without exception")
  void testValidate_success() {
    AIProvider noop = mockProvider("noop");
    AIGatewayProperties props = AIGatewayProperties.builder()
        .defaultProvider("noop")
        .defaultModel("noop-default")
        .model("noop-default", modelFor("noop"))
        .build();

    ProviderRegistry providerRegistry = new ProviderRegistry(List.of(noop), props);
    ModelRegistry modelRegistry = new ModelRegistry(props);
    AIRegistryValidator validator = new AIRegistryValidator(props, modelRegistry, providerRegistry);

    assertThatCode(validator::validate).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Missing default provider registration causes validation failure")
  void testValidate_missingDefaultProvider_throws() {
    AIGatewayProperties props = AIGatewayProperties.builder()
        .defaultProvider("gemini")
        .defaultModel("noop-default")
        .model("noop-default", modelFor("noop"))
        .build();

    AIProvider noop = mockProvider("noop");
    ProviderRegistry providerRegistry = new ProviderRegistry(List.of(noop), props);
    ModelRegistry modelRegistry = new ModelRegistry(props);
    AIRegistryValidator validator = new AIRegistryValidator(props, modelRegistry, providerRegistry);

    assertThatThrownBy(validator::validate)
        .isInstanceOf(AIConfigurationException.class)
        .hasMessageContaining("gemini")
        .hasMessageContaining("AI_CFG_011");
  }

  @Test
  @DisplayName("Missing default model alias causes validation failure")
  void testValidate_missingDefaultModel_throws() {
    AIProvider noop = mockProvider("noop");
    AIGatewayProperties props = AIGatewayProperties.builder()
        .defaultProvider("noop")
        .defaultModel("missing-alias")
        .model("noop-default", modelFor("noop"))
        .build();

    ProviderRegistry providerRegistry = new ProviderRegistry(List.of(noop), props);
    ModelRegistry modelRegistry = new ModelRegistry(props);
    AIRegistryValidator validator = new AIRegistryValidator(props, modelRegistry, providerRegistry);

    assertThatThrownBy(validator::validate)
        .isInstanceOf(AIConfigurationException.class)
        .hasMessageContaining("missing-alias")
        .hasMessageContaining("AI_CFG_012");
  }

  @Test
  @DisplayName("Model referencing unregistered provider causes validation failure")
  void testValidate_modelReferencesUnknownProvider_throws() {
    AIProvider noop = mockProvider("noop");
    AIGatewayProperties props = AIGatewayProperties.builder()
        .defaultProvider("noop")
        .defaultModel("noop-default")
        .model("noop-default", modelFor("noop"))
        .model("bad-model", modelFor("nonexistent-provider"))
        .build();

    ProviderRegistry providerRegistry = new ProviderRegistry(List.of(noop), props);
    ModelRegistry modelRegistry = new ModelRegistry(props);
    AIRegistryValidator validator = new AIRegistryValidator(props, modelRegistry, providerRegistry);

    assertThatThrownBy(validator::validate)
        .isInstanceOf(AIConfigurationException.class)
        .hasMessageContaining("nonexistent-provider")
        .hasMessageContaining("AI_CFG_013");
  }
}
