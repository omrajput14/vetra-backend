package app.vetra.ai.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.vetra.ai.config.AIGatewayProperties;
import app.vetra.ai.config.AIGatewayProperties.ModelConfig;
import app.vetra.ai.config.AIGatewayProperties.ProviderConfig;
import app.vetra.ai.exception.AIConfigurationException;
import app.vetra.ai.exception.AIProviderUnavailableException;
import app.vetra.ai.model.AICapability;
import app.vetra.ai.provider.AIProvider;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ProviderRegistry}: provider registration, capability filtering, duplicate
 * detection, default resolution, and availability.
 */
class ProviderRegistryTest {

  private AIProvider mockProvider(String name, boolean available, Set<AICapability> caps) {
    AIProvider provider = mock(AIProvider.class);
    when(provider.providerName()).thenReturn(name);
    when(provider.isAvailable()).thenReturn(available);
    when(provider.supportedCapabilities()).thenReturn(caps);
    return provider;
  }

  private AIGatewayProperties propertiesWithDefault(String defaultProvider) {
    ProviderConfig cfg = new ProviderConfig();
    cfg.setName(defaultProvider);
    cfg.setEnabled(true);
    ModelConfig modelCfg = new ModelConfig();
    modelCfg.setProvider(defaultProvider);
    modelCfg.setModelId("test-model");
    modelCfg.setContextWindow(1024);
    modelCfg.setMaxOutputTokens(256);
    return AIGatewayProperties.builder()
        .defaultProvider(defaultProvider)
        .defaultModel("default-alias")
        .provider(cfg)
        .model("default-alias", modelCfg)
        .build();
  }

  @Test
  @DisplayName("Provider is discoverable by name after registration")
  void testFindByName_registered() {
    AIProvider noop = mockProvider("noop", true, Set.of());
    AIGatewayProperties props = propertiesWithDefault("noop");

    ProviderRegistry registry = new ProviderRegistry(List.of(noop), props);

    assertThat(registry.findByName("noop")).isPresent();
    assertThat(registry.findByName("missing")).isEmpty();
  }

  @Test
  @DisplayName("getDefault returns the configured default provider")
  void testGetDefault_returnsDefault() {
    AIProvider noop = mockProvider("noop", true, Set.of());
    AIGatewayProperties props = propertiesWithDefault("noop");

    ProviderRegistry registry = new ProviderRegistry(List.of(noop), props);

    assertThat(registry.getDefault().providerName()).isEqualTo("noop");
  }

  @Test
  @DisplayName("getDefault throws when default provider is not registered")
  void testGetDefault_notRegistered_throws() {
    AIGatewayProperties props = propertiesWithDefault("gemini");
    ProviderRegistry registry = new ProviderRegistry(List.of(), props);

    assertThatThrownBy(registry::getDefault)
        .isInstanceOf(AIProviderUnavailableException.class)
        .hasMessageContaining("gemini");
  }

  @Test
  @DisplayName("Duplicate provider name throws AIConfigurationException")
  void testDuplicateProviderName_throws() {
    AIProvider p1 = mockProvider("gemini", true, Set.of());
    AIProvider p2 = mockProvider("gemini", true, Set.of());
    AIGatewayProperties props = propertiesWithDefault("noop");

    assertThatThrownBy(() -> new ProviderRegistry(List.of(p1, p2), props))
        .isInstanceOf(AIConfigurationException.class)
        .hasMessageContaining("gemini");
  }

  @Test
  @DisplayName("findByCapabilities returns only available providers supporting required caps")
  void testFindByCapabilities_filtered() {
    AIProvider vision = mockProvider("vision-provider", true, Set.of(AICapability.VISION, AICapability.JSON_MODE));
    AIProvider text = mockProvider("text-provider", true, Set.of());
    AIProvider unavailable = mockProvider("dead-provider", false, Set.of(AICapability.VISION));

    AIGatewayProperties props = propertiesWithDefault("noop");
    ProviderRegistry registry = new ProviderRegistry(List.of(vision, text, unavailable), props);

    List<AIProvider> result = registry.findByCapabilities(Set.of(AICapability.VISION));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).providerName()).isEqualTo("vision-provider");
  }

  @Test
  @DisplayName("getAvailable returns only providers where isAvailable() is true")
  void testGetAvailable_onlyAvailable() {
    AIProvider available = mockProvider("available-provider", true, Set.of());
    AIProvider unavailable = mockProvider("dead-provider", false, Set.of());
    AIGatewayProperties props = propertiesWithDefault("noop");

    ProviderRegistry registry = new ProviderRegistry(List.of(available, unavailable), props);

    assertThat(registry.getAvailable()).hasSize(1);
    assertThat(registry.getAvailable().get(0).providerName()).isEqualTo("available-provider");
  }

  @Test
  @DisplayName("isRegistered returns true for known provider name")
  void testIsRegistered() {
    AIProvider noop = mockProvider("noop", true, Set.of());
    AIGatewayProperties props = propertiesWithDefault("noop");
    ProviderRegistry registry = new ProviderRegistry(List.of(noop), props);

    assertThat(registry.isRegistered("noop")).isTrue();
    assertThat(registry.isRegistered("openai")).isFalse();
  }
}
