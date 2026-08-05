package app.vetra.ai.registry;

import app.vetra.ai.config.AIGatewayProperties;
import app.vetra.ai.config.AIGatewayProperties.ModelConfig;
import app.vetra.ai.exception.AIConfigurationException;
import app.vetra.ai.model.AICapability;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Registry for all AI models declared in configuration. Populated at application startup from
 * {@link AIGatewayProperties#getModels()}. Provides alias-based lookup and capability-filtered
 * queries. No provider-specific logic or external calls are made here.
 */
@Component
public class ModelRegistry {

  private static final Logger log = LoggerFactory.getLogger(ModelRegistry.class);

  private final Map<String, ModelDescriptor> models;
  private final String defaultModelAlias;

  /**
   * Constructs the registry from gateway properties. Validates that no duplicate aliases exist.
   *
   * @param properties the gateway configuration properties
   * @throws AIConfigurationException if any model config is invalid or duplicate aliases are found
   */
  public ModelRegistry(AIGatewayProperties properties) {
    this.defaultModelAlias = properties.getDefaultModel();
    this.models = buildRegistry(properties.getModels());
    log.info(
        "ModelRegistry initialized with {} model(s). Default alias: '{}'",
        models.size(),
        defaultModelAlias);
  }

  /**
   * Looks up a model by its alias.
   *
   * @param alias the model alias (e.g., "diagnostics-fast")
   * @return an {@link Optional} containing the descriptor if found
   */
  public Optional<ModelDescriptor> findByAlias(String alias) {
    return Optional.ofNullable(models.get(alias));
  }

  /**
   * Returns the descriptor for the configured default model alias.
   *
   * @return the default model descriptor
   * @throws AIConfigurationException if the default model alias is not registered
   */
  public ModelDescriptor getDefault() {
    return findByAlias(defaultModelAlias)
        .orElseThrow(
            () ->
                new AIConfigurationException(
                    "Default model alias '"
                        + defaultModelAlias
                        + "' is not registered in the ModelRegistry.",
                    "AI_CFG_002"));
  }

  /**
   * Returns all enabled models that support all of the requested capabilities.
   *
   * @param required the set of capabilities that must be supported
   * @return collection of matching enabled model descriptors
   */
  public Collection<ModelDescriptor> findByCapabilities(Set<AICapability> required) {
    return models.values().stream()
        .filter(ModelDescriptor::enabled)
        .filter(m -> m.supports(required))
        .collect(Collectors.toList());
  }

  /**
   * Returns all registered model descriptors.
   *
   * @return collection of all model descriptors
   */
  public Collection<ModelDescriptor> getAll() {
    return models.values();
  }

  /**
   * Returns true if a model with the given alias is registered.
   *
   * @param alias the model alias to check
   * @return true if registered
   */
  public boolean isRegistered(String alias) {
    return models.containsKey(alias);
  }

  private Map<String, ModelDescriptor> buildRegistry(Map<String, ModelConfig> modelConfigs) {
    Map<String, ModelDescriptor> registry = new LinkedHashMap<>();
    for (Map.Entry<String, ModelConfig> entry : modelConfigs.entrySet()) {
      String alias = entry.getKey();
      ModelConfig cfg = entry.getValue();

      if (registry.containsKey(alias)) {
        throw new AIConfigurationException(
            "Duplicate model alias '" + alias + "' detected in vetra.ai.gateway.models.",
            "AI_CFG_003");
      }

      Set<AICapability> capabilities = resolveCapabilities(alias, cfg);
      ModelDescriptor descriptor =
          new ModelDescriptor(
              alias,
              cfg.getModelId(),
              cfg.getProvider(),
              capabilities,
              cfg.getContextWindow(),
              cfg.getMaxOutputTokens(),
              cfg.isEnabled());

      registry.put(alias, descriptor);
      log.debug(
          "Registered model alias='{}' provider='{}' modelId='{}' capabilities={} enabled={}",
          alias,
          cfg.getProvider(),
          cfg.getModelId(),
          capabilities,
          cfg.isEnabled());
    }
    return registry;
  }

  private Set<AICapability> resolveCapabilities(String alias, ModelConfig cfg) {
    Set<AICapability> resolved =
        cfg.getCapabilities().stream()
            .map(
                name -> {
                  try {
                    return AICapability.valueOf(name.toUpperCase());
                  } catch (IllegalArgumentException e) {
                    throw new AIConfigurationException(
                        "Unknown capability '"
                            + name
                            + "' declared for model alias '"
                            + alias
                            + "'. Valid values: VISION, JSON_MODE, FUNCTION_CALLING, STREAMING,"
                            + " LONG_CONTEXT.",
                        "AI_CFG_004");
                  }
                })
            .collect(Collectors.toSet());

    // Supplement from boolean flags for ergonomics
    if (cfg.isSupportsVision()) {
      resolved.add(AICapability.VISION);
    }
    if (cfg.isSupportsJsonMode()) {
      resolved.add(AICapability.JSON_MODE);
    }
    if (cfg.isSupportsStreaming()) {
      resolved.add(AICapability.STREAMING);
    }

    return Set.copyOf(resolved);
  }
}
