package app.vetra.ai.registry;

import app.vetra.ai.config.AIGatewayProperties;
import app.vetra.ai.exception.AIProviderUnavailableException;
import app.vetra.ai.model.AICapability;
import app.vetra.ai.model.AIRequest;
import app.vetra.ai.provider.AIProvider;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Capability-aware provider router. Selects the most suitable {@link AIProvider} and {@link
 * ModelDescriptor} for a given {@link AIRequest} based on required capabilities and configured
 * defaults. No retry or failover logic is performed here — this component is responsible only for
 * the selection decision.
 */
@Component
public class ProviderRouter {

  private static final Logger log = LoggerFactory.getLogger(ProviderRouter.class);

  private final ProviderRegistry providerRegistry;
  private final ModelRegistry modelRegistry;
  private final AIGatewayProperties properties;

  /**
   * Constructs the router with its required registries and gateway properties.
   *
   * @param providerRegistry the provider registry
   * @param modelRegistry the model registry
   * @param properties the gateway configuration
   */
  public ProviderRouter(
      ProviderRegistry providerRegistry,
      ModelRegistry modelRegistry,
      AIGatewayProperties properties) {
    this.providerRegistry = providerRegistry;
    this.modelRegistry = modelRegistry;
    this.properties = properties;
  }

  /**
   * Resolves the most suitable routing decision for the given request. Selection priority:
   *
   * <ol>
   *   <li>If the request specifies required capabilities, filter providers and models by those
   *       capabilities.
   *   <li>Among capable candidates, prefer the default provider and model.
   *   <li>Fall back to any available capable provider if the default is unavailable.
   * </ol>
   *
   * @param request the incoming AI request
   * @return a {@link RoutingDecision} containing the selected provider and model
   * @throws AIProviderUnavailableException if no suitable provider/model pair can be found
   */
  public RoutingDecision route(AIRequest request) {
    return route(request, Set.of());
  }

  /**
   * Resolves the most suitable routing decision for the request, excluding any providers specified
   * in {@code excludedProviders}.
   *
   * @param request the incoming AI request
   * @param excludedProviders set of provider names (case-insensitive) to skip during selection
   * @return a {@link RoutingDecision}
   * @throws AIProviderUnavailableException if no non-excluded provider is available
   */
  public RoutingDecision route(AIRequest request, Set<String> excludedProviders) {
    Set<String> excluded =
        excludedProviders != null
            ? excludedProviders.stream()
                .map(String::toLowerCase)
                .collect(java.util.stream.Collectors.toSet())
            : Set.of();

    if (request.requestedProvider() != null
        && !excluded.contains(request.requestedProvider().name().toLowerCase())) {
      Optional<AIProvider> explicit =
          providerRegistry.findByName(request.requestedProvider().name());
      if (explicit.isPresent() && explicit.get().isAvailable()) {
        Optional<ModelDescriptor> explicitModel =
            modelRegistry.getAll().stream()
                .filter(m -> m.providerName().equalsIgnoreCase(request.requestedProvider().name()))
                .findFirst();
        if (explicitModel.isPresent()) {
          return new RoutingDecision(explicit.get(), explicitModel.get());
        }
      }
    }

    Set<AICapability> required = request.requiredCapabilities();
    List<ModelDescriptor> capableModels =
        modelRegistry.findByCapabilities(required).stream()
            .filter(m -> providerRegistry.isRegistered(m.providerName()))
            .filter(m -> !excluded.contains(m.providerName().toLowerCase()))
            .filter(
                m ->
                    providerRegistry
                        .findByName(m.providerName())
                        .map(AIProvider::isAvailable)
                        .orElse(false))
            .toList();

    if (capableModels.isEmpty()) {
      throw new AIProviderUnavailableException(
          "No available AI provider found supporting capabilities: "
              + required
              + " (excluded: "
              + excluded
              + ")",
          "none");
    }

    String defaultAlias = properties.getDefaultModel();
    Optional<ModelDescriptor> preferred =
        capableModels.stream().filter(m -> m.alias().equalsIgnoreCase(defaultAlias)).findFirst();

    ModelDescriptor selectedModel = preferred.orElse(capableModels.get(0));
    AIProvider selectedProvider =
        providerRegistry
            .findByName(selectedModel.providerName())
            .orElseThrow(
                () ->
                    new AIProviderUnavailableException(
                        "Provider '" + selectedModel.providerName() + "' not found in registry.",
                        selectedModel.providerName()));

    return new RoutingDecision(selectedProvider, selectedModel);
  }

  // ── Nested: RoutingDecision ───────────────────────────────────────────────

  /**
   * The result of a routing decision: an immutable pair of the selected provider and model
   * descriptor.
   *
   * @param provider the selected {@link AIProvider}
   * @param model the selected {@link ModelDescriptor}
   */
  public record RoutingDecision(AIProvider provider, ModelDescriptor model) {}
}
