package app.vetra.ai.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.vetra.ai.config.AIGatewayProperties;
import app.vetra.ai.config.AIGatewayProperties.ModelConfig;
import app.vetra.ai.exception.AIConfigurationException;
import app.vetra.ai.model.AICapability;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ModelRegistry}: alias lookup, capability filtering, default resolution,
 * and duplicate detection.
 */
class ModelRegistryTest {

  private AIGatewayProperties buildProperties(String defaultModel, Map<String, ModelConfig> models) {
    return AIGatewayProperties.builder()
        .defaultProvider("noop")
        .defaultModel(defaultModel)
        .model("noop-default", noopModel())
        .build();
  }

  private ModelConfig visionModel(String provider) {
    ModelConfig cfg = new ModelConfig();
    cfg.setProvider(provider);
    cfg.setModelId("test-model-vision");
    cfg.setCapabilities(java.util.List.of("VISION", "JSON_MODE"));
    cfg.setContextWindow(4096);
    cfg.setMaxOutputTokens(1024);
    cfg.setEnabled(true);
    return cfg;
  }

  private ModelConfig noopModel() {
    ModelConfig cfg = new ModelConfig();
    cfg.setProvider("noop");
    cfg.setModelId("noop-v1");
    cfg.setContextWindow(1);
    cfg.setMaxOutputTokens(1);
    cfg.setEnabled(true);
    return cfg;
  }

  private AIGatewayProperties propertiesWithModels(String defaultModel, Map<String, ModelConfig> models) {
    AIGatewayProperties.Builder builder = AIGatewayProperties.builder()
        .defaultProvider("noop")
        .defaultModel(defaultModel);
    models.forEach(builder::model);
    return builder.build();
  }

  @Test
  @DisplayName("findByAlias returns descriptor for known alias")
  void testFindByAlias_found() {
    AIGatewayProperties props = propertiesWithModels("noop-default", Map.of("noop-default", noopModel()));
    ModelRegistry registry = new ModelRegistry(props);

    Optional<ModelDescriptor> result = registry.findByAlias("noop-default");

    assertThat(result).isPresent();
    assertThat(result.get().alias()).isEqualTo("noop-default");
    assertThat(result.get().providerName()).isEqualTo("noop");
  }

  @Test
  @DisplayName("findByAlias returns empty for unknown alias")
  void testFindByAlias_notFound() {
    AIGatewayProperties props = propertiesWithModels("noop-default", Map.of("noop-default", noopModel()));
    ModelRegistry registry = new ModelRegistry(props);

    assertThat(registry.findByAlias("unknown-alias")).isEmpty();
  }

  @Test
  @DisplayName("getDefault returns descriptor for configured default alias")
  void testGetDefault_success() {
    AIGatewayProperties props = propertiesWithModels("noop-default", Map.of("noop-default", noopModel()));
    ModelRegistry registry = new ModelRegistry(props);

    ModelDescriptor desc = registry.getDefault();

    assertThat(desc.alias()).isEqualTo("noop-default");
    assertThat(desc.modelId()).isEqualTo("noop-v1");
  }

  @Test
  @DisplayName("getDefault throws when default alias is not registered")
  void testGetDefault_missingAlias() {
    AIGatewayProperties props = propertiesWithModels("missing-alias", Map.of("noop-default", noopModel()));
    ModelRegistry registry = new ModelRegistry(props);

    assertThatThrownBy(registry::getDefault)
        .isInstanceOf(AIConfigurationException.class)
        .hasMessageContaining("missing-alias");
  }

  @Test
  @DisplayName("findByCapabilities returns models supporting all required capabilities")
  void testFindByCapabilities_match() {
    Map<String, ModelConfig> models = Map.of(
        "noop-default", noopModel(),
        "vision-model", visionModel("test-provider"));
    AIGatewayProperties props = propertiesWithModels("noop-default", models);
    ModelRegistry registry = new ModelRegistry(props);

    Collection<ModelDescriptor> result = registry.findByCapabilities(Set.of(AICapability.VISION));

    assertThat(result).hasSize(1);
    assertThat(result.iterator().next().alias()).isEqualTo("vision-model");
  }

  @Test
  @DisplayName("findByCapabilities returns empty when no model matches")
  void testFindByCapabilities_noMatch() {
    AIGatewayProperties props = propertiesWithModels("noop-default", Map.of("noop-default", noopModel()));
    ModelRegistry registry = new ModelRegistry(props);

    Collection<ModelDescriptor> result = registry.findByCapabilities(Set.of(AICapability.STREAMING));

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("isRegistered returns true for known alias")
  void testIsRegistered_known() {
    AIGatewayProperties props = propertiesWithModels("noop-default", Map.of("noop-default", noopModel()));
    ModelRegistry registry = new ModelRegistry(props);

    assertThat(registry.isRegistered("noop-default")).isTrue();
    assertThat(registry.isRegistered("not-real")).isFalse();
  }

  @Test
  @DisplayName("Unknown capability string in config throws AIConfigurationException")
  void testUnknownCapabilityString_throws() {
    ModelConfig badCfg = new ModelConfig();
    badCfg.setProvider("noop");
    badCfg.setModelId("bad-model");
    badCfg.setCapabilities(java.util.List.of("INVALID_CAPABILITY"));
    badCfg.setContextWindow(1024);
    badCfg.setMaxOutputTokens(256);
    badCfg.setEnabled(true);

    AIGatewayProperties props = propertiesWithModels("noop-default",
        Map.of("noop-default", noopModel(), "bad-model", badCfg));

    assertThatThrownBy(() -> new ModelRegistry(props))
        .isInstanceOf(AIConfigurationException.class)
        .hasMessageContaining("INVALID_CAPABILITY");
  }

  @Test
  @DisplayName("Boolean vision flag is resolved to VISION capability")
  void testBooleanVisionFlag_resolvedToCapability() {
    ModelConfig cfg = new ModelConfig();
    cfg.setProvider("noop");
    cfg.setModelId("vision-only");
    cfg.setContextWindow(4096);
    cfg.setMaxOutputTokens(1024);
    cfg.setSupportsVision(true);
    cfg.setEnabled(true);

    AIGatewayProperties props = propertiesWithModels("noop-default",
        Map.of("noop-default", noopModel(), "vision-only", cfg));
    ModelRegistry registry = new ModelRegistry(props);

    ModelDescriptor desc = registry.findByAlias("vision-only").orElseThrow();
    assertThat(desc.capabilities()).contains(AICapability.VISION);
  }
}
