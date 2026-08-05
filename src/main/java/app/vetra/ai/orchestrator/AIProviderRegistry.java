package app.vetra.ai.orchestrator;

import app.vetra.ai.config.AIProperties;
import app.vetra.ai.entity.AIProviderType;
import app.vetra.ai.exception.AIProviderUnavailableException;
import app.vetra.ai.provider.AIProvider;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Registry component auto-discovering, registering, and providing lookup for AIProvider strategies.
 */
@Component
public class AIProviderRegistry {

  private static final Logger log = LoggerFactory.getLogger(AIProviderRegistry.class);

  private final Map<AIProviderType, AIProvider> providerMap = new EnumMap<>(AIProviderType.class);
  private final AIProperties aiProperties;

  /**
   * Auto-discovers all AIProvider Spring beans and populates registry map.
   *
   * @param providers spring injected list of AIProvider implementations
   * @param aiProperties configuration properties
   */
  public AIProviderRegistry(List<AIProvider> providers, AIProperties aiProperties) {
    this.aiProperties = aiProperties;
    for (AIProvider provider : providers) {
      providerMap.put(provider.providerType(), provider);
      log.info(
          "Registered AI Provider: [{}] type={}", provider.providerName(), provider.providerType());
    }
  }

  /**
   * Retrieves provider by provider type enum or falls back to default configured provider.
   *
   * @param type requested provider type (optional)
   * @return {@link AIProvider} instance
   */
  public AIProvider getProvider(AIProviderType type) {
    AIProviderType targetType =
        (type != null && type != AIProviderType.NONE) ? type : aiProperties.getDefaultProvider();

    return Optional.ofNullable(providerMap.get(targetType))
        .orElseGet(
            () ->
                Optional.ofNullable(providerMap.get(AIProviderType.NONE))
                    .orElseThrow(
                        () ->
                            new AIProviderUnavailableException(
                                "No registered AI provider found for type: " + targetType,
                                "AI_003")));
  }

  /**
   * Retrieves the configured default AI provider.
   *
   * @return default {@link AIProvider}
   */
  public AIProvider getDefaultProvider() {
    return getProvider(aiProperties.getDefaultProvider());
  }

  /**
   * Returns whether the AI platform is globally enabled in configuration.
   *
   * @return true if enabled
   */
  public boolean isPlatformEnabled() {
    return aiProperties.isEnabled();
  }

  /**
   * Retrieves list of all registered provider types.
   *
   * @return list of supported {@link AIProviderType}
   */
  public List<AIProviderType> getRegisteredTypes() {
    return List.copyOf(providerMap.keySet());
  }

  /**
   * Retrieves list of currently available providers.
   *
   * @return list of active {@link AIProvider}
   */
  public List<AIProvider> getAvailableProviders() {
    return providerMap.values().stream().filter(AIProvider::isAvailable).toList();
  }
}
