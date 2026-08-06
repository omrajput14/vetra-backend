package app.vetra.ai.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Strongly-typed, immutable configuration properties for the AI Gateway layer. Binds from the
 * {@code vetra.ai.gateway} YAML namespace.
 */
@ConfigurationProperties(prefix = "vetra.ai.gateway")
public final class AIGatewayProperties {

  private final boolean enabled;
  private final String defaultProvider;
  private final String defaultModel;
  private final Duration timeout;
  private final List<ProviderConfig> providers;
  private final Map<String, ModelConfig> models;
  private final GovernanceProperties governance;

  /**
   * Spring-constructor-binding constructor.
   *
   * @param enabled enabled status
   * @param defaultProvider default provider
   * @param defaultModel default model
   * @param timeout timeout duration
   * @param providers list of provider configs
   * @param models map of model configs
   * @param governance governance configuration
   */
  public AIGatewayProperties(
      @DefaultValue("false") boolean enabled,
      @DefaultValue("noop") String defaultProvider,
      @DefaultValue("noop-default") String defaultModel,
      @DefaultValue("10s") Duration timeout,
      @DefaultValue List<ProviderConfig> providers,
      @DefaultValue Map<String, ModelConfig> models,
      @DefaultValue GovernanceProperties governance) {
    this.enabled = enabled;
    this.defaultProvider = defaultProvider;
    this.defaultModel = defaultModel;
    this.timeout = timeout;
    this.providers = providers != null ? List.copyOf(providers) : List.of();
    this.models = models != null ? Map.copyOf(models) : Map.of();
    this.governance = governance != null ? governance : new GovernanceProperties();
  }

  /**
   * Returns true if gateway is enabled.
   *
   * @return true if enabled
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Returns default provider.
   *
   * @return default provider name
   */
  public String getDefaultProvider() {
    return defaultProvider;
  }

  /**
   * Returns default model.
   *
   * @return default model name
   */
  public String getDefaultModel() {
    return defaultModel;
  }

  /**
   * Returns timeout.
   *
   * @return timeout duration
   */
  public Duration getTimeout() {
    return timeout;
  }

  /**
   * Returns provider configs.
   *
   * @return list of provider configs
   */
  public List<ProviderConfig> getProviders() {
    return providers;
  }

  /**
   * Returns model configs.
   *
   * @return map of model configs
   */
  public Map<String, ModelConfig> getModels() {
    return models;
  }

  /**
   * Returns governance properties.
   *
   * @return governance properties object
   */
  public GovernanceProperties getGovernance() {
    return governance;
  }

  // ── Nested: ProviderConfig ────────────────────────────────────────────────

  /** Configuration entry for a single AI provider. */
  public static final class ProviderConfig {

    private String name;
    private boolean enabled = true;
    private int priority = 1;
    private ResilienceConfig resilience = new ResilienceConfig();

    /** Default constructor. */
    public ProviderConfig() {}

    /**
     * Returns provider name.
     *
     * @return provider name
     */
    public String getName() {
      return name;
    }

    /**
     * Sets provider name.
     *
     * @param name provider name
     */
    public void setName(String name) {
      this.name = name;
    }

    /**
     * Returns whether provider is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
      return enabled;
    }

    /**
     * Sets enabled status.
     *
     * @param enabled true to enable
     */
    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    /**
     * Returns priority.
     *
     * @return priority value
     */
    public int getPriority() {
      return priority;
    }

    /**
     * Sets priority.
     *
     * @param priority priority value
     */
    public void setPriority(int priority) {
      this.priority = priority;
    }

    /**
     * Returns resilience config.
     *
     * @return resilience config
     */
    public ResilienceConfig getResilience() {
      return resilience;
    }

    /**
     * Sets resilience config.
     *
     * @param resilience resilience config
     */
    public void setResilience(ResilienceConfig resilience) {
      this.resilience = resilience != null ? resilience : new ResilienceConfig();
    }
  }

  // ── Nested: ModelConfig ───────────────────────────────────────────────────

  /** Configuration entry for a single model alias. */
  public static final class ModelConfig {

    private String provider;
    private String modelId;
    private int contextWindow = 8192;
    private int maxOutputTokens = 2048;
    private boolean supportsVision = false;
    private boolean supportsStreaming = false;
    private boolean supportsJsonMode = false;
    private boolean enabled = true;
    private List<String> capabilities = new ArrayList<>();

    /** Default constructor. */
    public ModelConfig() {}

    /**
     * Returns provider name.
     *
     * @return provider name
     */
    public String getProvider() {
      return provider;
    }

    /**
     * Sets provider name.
     *
     * @param provider provider name
     */
    public void setProvider(String provider) {
      this.provider = provider;
    }

    /**
     * Returns model ID.
     *
     * @return model ID
     */
    public String getModelId() {
      return modelId;
    }

    /**
     * Sets model ID.
     *
     * @param modelId model ID
     */
    public void setModelId(String modelId) {
      this.modelId = modelId;
    }

    /**
     * Returns context window token size.
     *
     * @return token count
     */
    public int getContextWindow() {
      return contextWindow;
    }

    /**
     * Sets context window size.
     *
     * @param contextWindow token count
     */
    public void setContextWindow(int contextWindow) {
      this.contextWindow = contextWindow;
    }

    /**
     * Returns max output tokens.
     *
     * @return max output token count
     */
    public int getMaxOutputTokens() {
      return maxOutputTokens;
    }

    /**
     * Sets max output tokens.
     *
     * @param maxOutputTokens max token count
     */
    public void setMaxOutputTokens(int maxOutputTokens) {
      this.maxOutputTokens = maxOutputTokens;
    }

    /**
     * Returns true if vision supported.
     *
     * @return true if supported
     */
    public boolean isSupportsVision() {
      return supportsVision;
    }

    /**
     * Sets vision support.
     *
     * @param supportsVision true if supported
     */
    public void setSupportsVision(boolean supportsVision) {
      this.supportsVision = supportsVision;
    }

    /**
     * Returns true if streaming supported.
     *
     * @return true if supported
     */
    public boolean isSupportsStreaming() {
      return supportsStreaming;
    }

    /**
     * Sets streaming support.
     *
     * @param supportsStreaming true if supported
     */
    public void setSupportsStreaming(boolean supportsStreaming) {
      this.supportsStreaming = supportsStreaming;
    }

    /**
     * Returns true if JSON mode supported.
     *
     * @return true if supported
     */
    public boolean isSupportsJsonMode() {
      return supportsJsonMode;
    }

    /**
     * Sets JSON mode support.
     *
     * @param supportsJsonMode true if supported
     */
    public void setSupportsJsonMode(boolean supportsJsonMode) {
      this.supportsJsonMode = supportsJsonMode;
    }

    /**
     * Returns true if enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
      return enabled;
    }

    /**
     * Sets enabled status.
     *
     * @param enabled true to enable
     */
    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    /**
     * Returns capabilities list.
     *
     * @return list of capability strings
     */
    public List<String> getCapabilities() {
      return capabilities;
    }

    /**
     * Sets capabilities list.
     *
     * @param capabilities list of capability strings
     */
    public void setCapabilities(List<String> capabilities) {
      this.capabilities = capabilities != null ? capabilities : new ArrayList<>();
    }
  }

  /**
   * Creates builder.
   *
   * @return new Builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for constructing AIGatewayProperties in tests. */
  public static final class Builder {

    private boolean enabled = true;
    private String defaultProvider = "noop";
    private String defaultModel = "noop-default";
    private Duration timeout = Duration.ofSeconds(10);
    private List<ProviderConfig> providers = new ArrayList<>();
    private Map<String, ModelConfig> models = new LinkedHashMap<>();
    private GovernanceProperties governance = new GovernanceProperties();

    /** Default constructor. */
    public Builder() {}

    /**
     * Sets enabled status.
     *
     * @param enabled true to enable
     * @return builder instance
     */
    public Builder enabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    /**
     * Sets default provider.
     *
     * @param defaultProvider provider name
     * @return builder instance
     */
    public Builder defaultProvider(String defaultProvider) {
      this.defaultProvider = defaultProvider;
      return this;
    }

    /**
     * Sets default model.
     *
     * @param defaultModel model alias
     * @return builder instance
     */
    public Builder defaultModel(String defaultModel) {
      this.defaultModel = defaultModel;
      return this;
    }

    /**
     * Sets timeout.
     *
     * @param timeout timeout duration
     * @return builder instance
     */
    public Builder timeout(Duration timeout) {
      this.timeout = timeout;
      return this;
    }

    /**
     * Adds provider config.
     *
     * @param config provider config
     * @return builder instance
     */
    public Builder provider(ProviderConfig config) {
      this.providers.add(config);
      return this;
    }

    /**
     * Adds model config.
     *
     * @param alias model alias
     * @param config model config
     * @return builder instance
     */
    public Builder model(String alias, ModelConfig config) {
      this.models.put(alias, config);
      return this;
    }

    /**
     * Sets governance properties.
     *
     * @param governance governance properties object
     * @return builder instance
     */
    public Builder governance(GovernanceProperties governance) {
      this.governance = governance;
      return this;
    }

    /**
     * Builds AIGatewayProperties.
     *
     * @return constructed properties
     */
    public AIGatewayProperties build() {
      return new AIGatewayProperties(
          enabled, defaultProvider, defaultModel, timeout, providers, models, governance);
    }
  }
}
