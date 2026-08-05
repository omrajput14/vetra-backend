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
 * {@code vetra.ai.gateway} YAML namespace. No provider-specific terminology is permitted here —
 * this configuration layer is provider-agnostic by design.
 *
 * <p>Example YAML:
 *
 * <pre>{@code
 * vetra:
 *   ai:
 *     gateway:
 *       default-provider: gemini
 *       default-model: diagnostics-fast
 *       timeout: 10s
 *       models:
 *         diagnostics-fast:
 *           provider: gemini
 *           model-id: gemini-2.5-flash
 *           capabilities: [VISION, JSON_MODE]
 *           context-window: 1048576
 *           max-output-tokens: 8192
 *           enabled: true
 * }</pre>
 */
@ConfigurationProperties(prefix = "vetra.ai.gateway")
public final class AIGatewayProperties {

  private final boolean enabled;

  private final String defaultProvider;

  private final String defaultModel;

  private final Duration timeout;

  private final List<ProviderConfig> providers;

  private final Map<String, ModelConfig> models;

  /** Spring-constructor-binding constructor. */
  public AIGatewayProperties(
      @DefaultValue("false") boolean enabled,
      @DefaultValue("noop") String defaultProvider,
      @DefaultValue("noop-default") String defaultModel,
      @DefaultValue("10s") Duration timeout,
      @DefaultValue List<ProviderConfig> providers,
      @DefaultValue Map<String, ModelConfig> models) {
    this.enabled = enabled;
    this.defaultProvider = defaultProvider;
    this.defaultModel = defaultModel;
    this.timeout = timeout;
    this.providers = providers != null ? List.copyOf(providers) : List.of();
    this.models = models != null ? Map.copyOf(models) : Map.of();
  }

  /**
   * Returns true if the AI Gateway routing layer is enabled.
   *
   * @return true if gateway is enabled
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Returns the name of the default provider.
   *
   * @return default provider name
   */
  public String getDefaultProvider() {
    return defaultProvider;
  }

  /**
   * Returns the alias of the default model.
   *
   * @return default model alias
   */
  public String getDefaultModel() {
    return defaultModel;
  }

  /**
   * Returns the request execution timeout for the gateway.
   *
   * @return timeout duration
   */
  public Duration getTimeout() {
    return timeout;
  }

  /**
   * Returns the list of provider configuration entries.
   *
   * @return immutable list of provider configs
   */
  public List<ProviderConfig> getProviders() {
    return providers;
  }

  /**
   * Returns the map of model alias to model configuration.
   *
   * @return immutable map of model configs keyed by alias
   */
  public Map<String, ModelConfig> getModels() {
    return models;
  }

  // ── Nested: ProviderConfig ────────────────────────────────────────────────

  /**
   * Configuration entry for a single AI provider. Provider names must be unique across all entries.
   */
  public static final class ProviderConfig {

    private String name;

    private boolean enabled = true;

    /** No-arg constructor for property binding. */
    public ProviderConfig() {}

    /**
     * Returns the unique provider name (matches {@code AIProvider.providerName()}).
     *
     * @return provider name
     */
    public String getName() {
      return name;
    }

    /**
     * Sets the provider name.
     *
     * @param name the provider name
     */
    public void setName(String name) {
      this.name = name;
    }

    /**
     * Returns whether this provider is enabled for traffic.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
      return enabled;
    }

    /**
     * Sets the enabled status of this provider.
     *
     * @param enabled true to enable
     */
    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }

  // ── Nested: ModelConfig ───────────────────────────────────────────────────

  /**
   * Configuration entry for a single model alias. The alias is the key in the {@code models} map.
   * Model IDs are provider-specific and must not be referenced directly by business logic.
   */
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

    /** No-arg constructor for property binding. */
    public ModelConfig() {}

    /**
     * Returns the provider name this model belongs to.
     *
     * @return provider name
     */
    public String getProvider() {
      return provider;
    }

    /**
     * Sets the provider name.
     *
     * @param provider the provider name
     */
    public void setProvider(String provider) {
      this.provider = provider;
    }

    /**
     * Returns the provider-specific model identifier (e.g., gemini-2.5-flash).
     *
     * @return model ID
     */
    public String getModelId() {
      return modelId;
    }

    /**
     * Sets the model ID.
     *
     * @param modelId the model ID
     */
    public void setModelId(String modelId) {
      this.modelId = modelId;
    }

    /**
     * Returns the maximum context window in tokens.
     *
     * @return context window size
     */
    public int getContextWindow() {
      return contextWindow;
    }

    /**
     * Sets the context window size.
     *
     * @param contextWindow context window in tokens
     */
    public void setContextWindow(int contextWindow) {
      this.contextWindow = contextWindow;
    }

    /**
     * Returns the maximum number of output tokens.
     *
     * @return max output tokens
     */
    public int getMaxOutputTokens() {
      return maxOutputTokens;
    }

    /**
     * Sets the maximum output tokens.
     *
     * @param maxOutputTokens max output tokens
     */
    public void setMaxOutputTokens(int maxOutputTokens) {
      this.maxOutputTokens = maxOutputTokens;
    }

    /**
     * Returns true if this model supports vision (image) inputs.
     *
     * @return true if vision is supported
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
     * Returns true if this model supports streaming responses.
     *
     * @return true if streaming is supported
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
     * Returns true if this model supports native JSON mode.
     *
     * @return true if JSON mode is supported
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
     * Returns true if this model is enabled for use.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
      return enabled;
    }

    /**
     * Sets the enabled status.
     *
     * @param enabled true to enable
     */
    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    /**
     * Returns the list of capability names declared for this model (e.g., "VISION", "JSON_MODE").
     *
     * @return list of capability name strings
     */
    public List<String> getCapabilities() {
      return capabilities;
    }

    /**
     * Sets the capability list.
     *
     * @param capabilities list of capability name strings
     */
    public void setCapabilities(List<String> capabilities) {
      this.capabilities = capabilities != null ? capabilities : new ArrayList<>();
    }
  }

  /**
   * Returns a new {@link Builder} for constructing {@link AIGatewayProperties} in tests.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for constructing {@link AIGatewayProperties} in unit tests. */
  public static final class Builder {

    private boolean enabled = true;
    private String defaultProvider = "noop";
    private String defaultModel = "noop-default";
    private Duration timeout = Duration.ofSeconds(10);
    private List<ProviderConfig> providers = new ArrayList<>();
    private Map<String, ModelConfig> models = new LinkedHashMap<>();

    /**
     * Sets whether the gateway is enabled.
     *
     * @param enabled true to enable
     * @return this builder
     */
    public Builder enabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    /**
     * Sets the default provider name.
     *
     * @param defaultProvider provider name
     * @return this builder
     */
    public Builder defaultProvider(String defaultProvider) {
      this.defaultProvider = defaultProvider;
      return this;
    }

    /**
     * Sets the default model alias.
     *
     * @param defaultModel model alias
     * @return this builder
     */
    public Builder defaultModel(String defaultModel) {
      this.defaultModel = defaultModel;
      return this;
    }

    /**
     * Sets the gateway timeout.
     *
     * @param timeout request timeout
     * @return this builder
     */
    public Builder timeout(Duration timeout) {
      this.timeout = timeout;
      return this;
    }

    /**
     * Adds a provider configuration.
     *
     * @param config provider config
     * @return this builder
     */
    public Builder provider(ProviderConfig config) {
      this.providers.add(config);
      return this;
    }

    /**
     * Adds a model configuration with the given alias.
     *
     * @param alias model alias key
     * @param config model config
     * @return this builder
     */
    public Builder model(String alias, ModelConfig config) {
      this.models.put(alias, config);
      return this;
    }

    /**
     * Builds and returns the {@link AIGatewayProperties} instance.
     *
     * @return constructed properties
     */
    public AIGatewayProperties build() {
      return new AIGatewayProperties(
          enabled, defaultProvider, defaultModel, timeout, providers, models);
    }
  }
}
