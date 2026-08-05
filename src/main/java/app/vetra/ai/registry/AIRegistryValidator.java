package app.vetra.ai.registry;

import app.vetra.ai.config.AIGatewayProperties;
import app.vetra.ai.config.AIGatewayProperties.ModelConfig;
import app.vetra.ai.exception.AIConfigurationException;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Performs fail-fast startup validation of the AI Gateway configuration. Only executes when {@code
 * vetra.ai.gateway.enabled=true}. When the gateway is disabled (the default), this validator is a
 * no-op and the Spring context starts normally. This design ensures that integration tests and
 * non-gateway profiles are not affected by gateway configuration requirements.
 *
 * <p>When enabled, if any validation error is found, a single {@link AIConfigurationException} is
 * thrown containing all actionable error messages so that the application fails immediately with a
 * clear diagnosis rather than failing silently at request time.
 */
@Component
public class AIRegistryValidator {

  private static final Logger log = LoggerFactory.getLogger(AIRegistryValidator.class);

  private final AIGatewayProperties properties;
  private final ModelRegistry modelRegistry;
  private final ProviderRegistry providerRegistry;

  /**
   * Constructs the validator with its dependencies.
   *
   * @param properties the gateway configuration properties
   * @param modelRegistry the model registry
   * @param providerRegistry the provider registry
   */
  public AIRegistryValidator(
      AIGatewayProperties properties,
      ModelRegistry modelRegistry,
      ProviderRegistry providerRegistry) {
    this.properties = properties;
    this.modelRegistry = modelRegistry;
    this.providerRegistry = providerRegistry;
  }

  /**
   * Executes all validation rules after Spring context initialization. Skips validation when the
   * gateway is disabled ({@code vetra.ai.gateway.enabled=false}). When enabled, collects all errors
   * and throws a single exception containing the complete list of violations.
   *
   * @throws AIConfigurationException if gateway is enabled and any configuration violation exists
   */
  @PostConstruct
  public void validate() {
    if (!properties.isEnabled()) {
      log.debug(
          "AI Gateway is disabled (vetra.ai.gateway.enabled=false). Skipping registry validation.");
      return;
    }

    List<String> errors = new ArrayList<>();

    validateDefaultProvider(errors);
    validateDefaultModel(errors);
    validateModelProviderReferences(errors);

    if (!errors.isEmpty()) {
      String summary =
          "AI Gateway configuration validation failed with "
              + errors.size()
              + " error(s):\n"
              + String.join("\n", errors);
      log.error(summary);
      throw new AIConfigurationException(summary, "AI_CFG_010");
    }

    log.info(
        "AI Gateway configuration validated successfully. Providers: {} | Models: {}",
        providerRegistry.getAll().size(),
        modelRegistry.getAll().size());
  }

  private void validateDefaultProvider(List<String> errors) {
    String defaultProvider = properties.getDefaultProvider();
    if (defaultProvider == null || defaultProvider.isBlank()) {
      errors.add("[AI_CFG_011] vetra.ai.gateway.default-provider must not be blank.");
      return;
    }
    if (!providerRegistry.isRegistered(defaultProvider)) {
      errors.add(
          "[AI_CFG_011] Default provider '"
              + defaultProvider
              + "' is not registered. "
              + "Ensure an AIProvider bean with providerName() = '"
              + defaultProvider
              + "' exists.");
    }
  }

  private void validateDefaultModel(List<String> errors) {
    String defaultModel = properties.getDefaultModel();
    if (defaultModel == null || defaultModel.isBlank()) {
      errors.add("[AI_CFG_012] vetra.ai.gateway.default-model must not be blank.");
      return;
    }
    if (!modelRegistry.isRegistered(defaultModel)) {
      errors.add(
          "[AI_CFG_012] Default model alias '"
              + defaultModel
              + "' is not declared in vetra.ai.gateway.models. "
              + "Add an entry with this alias or update default-model.");
    }
  }

  private void validateModelProviderReferences(List<String> errors) {
    for (Map.Entry<String, ModelConfig> entry : properties.getModels().entrySet()) {
      String alias = entry.getKey();
      ModelConfig cfg = entry.getValue();
      String referencedProvider = cfg.getProvider();
      if (!providerRegistry.isRegistered(referencedProvider)) {
        errors.add(
            "[AI_CFG_013] Model alias '"
                + alias
                + "' references provider '"
                + referencedProvider
                + "' which is not registered. "
                + "Ensure an AIProvider bean with providerName() = '"
                + referencedProvider
                + "' exists.");
      }
    }
  }
}
