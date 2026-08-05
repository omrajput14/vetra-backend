package app.vetra.ai.registry;

import app.vetra.ai.config.AIGatewayProperties;
import app.vetra.ai.config.AIGatewayProperties.ProviderConfig;
import app.vetra.ai.exception.AIConfigurationException;
import app.vetra.ai.exception.AIProviderUnavailableException;
import app.vetra.ai.model.AICapability;
import app.vetra.ai.provider.AIProvider;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Registry for all AI providers discovered by Spring and validated against configuration. Providers
 * are registered by their {@link AIProvider#providerName()} identifier. This registry never casts
 * to concrete provider implementations — all interactions are through the {@link AIProvider}
 * interface. Startup validation ensures every configured provider name is matched by a discovered
 * bean.
 */
@Component
public class ProviderRegistry {

  private static final Logger log = LoggerFactory.getLogger(ProviderRegistry.class);

  private final Map<String, AIProvider> providers;
  private final String defaultProviderName;

  /**
   * Constructs the registry by matching Spring-discovered {@link AIProvider} beans against the
   * configured provider list.
   *
   * @param discoveredProviders Spring-injected list of all AIProvider beans
   * @param properties the gateway configuration properties
   * @throws AIConfigurationException if duplicate provider names are detected
   */
  public ProviderRegistry(List<AIProvider> discoveredProviders, AIGatewayProperties properties) {
    this.defaultProviderName = properties.getDefaultProvider();
    this.providers = buildRegistry(discoveredProviders, properties.getProviders());
    log.info(
        "ProviderRegistry initialized with {} provider(s). Default: '{}'",
        providers.size(),
        defaultProviderName);
  }

  /**
   * Looks up a provider by its name.
   *
   * @param name the provider name (e.g., "gemini", "noop")
   * @return an {@link Optional} containing the provider if found
   */
  public Optional<AIProvider> findByName(String name) {
    return Optional.ofNullable(providers.get(name));
  }

  /**
   * Returns the configured default provider.
   *
   * @return the default {@link AIProvider}
   * @throws AIProviderUnavailableException if the default provider is not registered
   */
  public AIProvider getDefault() {
    return findByName(defaultProviderName)
        .orElseThrow(
            () ->
                new AIProviderUnavailableException(
                    "Default provider '"
                        + defaultProviderName
                        + "' is not registered in the ProviderRegistry.",
                    defaultProviderName));
  }

  /**
   * Returns all enabled and available providers that support all required capabilities.
   *
   * @param required the set of capabilities the provider must support
   * @return list of compatible providers
   */
  public List<AIProvider> findByCapabilities(Set<AICapability> required) {
    return providers.values().stream()
        .filter(AIProvider::isAvailable)
        .filter(p -> p.supportedCapabilities().containsAll(required))
        .collect(Collectors.toList());
  }

  /**
   * Returns all registered providers regardless of availability.
   *
   * @return collection of all providers
   */
  public Collection<AIProvider> getAll() {
    return providers.values();
  }

  /**
   * Returns all currently available providers.
   *
   * @return list of available providers
   */
  public List<AIProvider> getAvailable() {
    return providers.values().stream().filter(AIProvider::isAvailable).collect(Collectors.toList());
  }

  /**
   * Returns true if a provider with the given name is registered.
   *
   * @param name the provider name to check
   * @return true if registered
   */
  public boolean isRegistered(String name) {
    return providers.containsKey(name);
  }

  private Map<String, AIProvider> buildRegistry(
      List<AIProvider> discovered, List<ProviderConfig> configuredProviders) {

    Map<String, AIProvider> registry = new LinkedHashMap<>();

    for (AIProvider provider : discovered) {
      String name = provider.providerName();
      if (registry.containsKey(name)) {
        throw new AIConfigurationException(
            "Duplicate provider name '"
                + name
                + "' detected. Each AIProvider bean must return a unique providerName().",
            "AI_CFG_001");
      }
      registry.put(name, provider);
      log.debug(
          "Discovered AI provider: name='{}' available={} capabilities={}",
          name,
          provider.isAvailable(),
          provider.supportedCapabilities());
    }

    // Validate that all explicitly configured providers have a matching discovered bean
    for (ProviderConfig cfg : configuredProviders) {
      if (!registry.containsKey(cfg.getName())) {
        log.warn(
            "Configured provider '{}' has no matching AIProvider bean registered. "
                + "It will be unavailable for routing.",
            cfg.getName());
      }
    }

    return registry;
  }
}
