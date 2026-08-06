package app.vetra.ai.gateway;

import app.vetra.ai.exception.AIConfigurationException;
import app.vetra.ai.gateway.governance.AIGovernancePipeline;
import app.vetra.ai.model.AICapability;
import app.vetra.ai.model.AIExecutionContext;
import app.vetra.ai.model.AIRequest;
import app.vetra.ai.model.AIResponse;
import app.vetra.ai.prompt.PromptDescriptor;
import app.vetra.ai.prompt.PromptRegistry;
import app.vetra.ai.prompt.PromptRenderer;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Pure orchestration facade for the AI Gateway.
 *
 * <p>Validates requests, resolves prompt templates, renders context variables, and passes execution
 * through {@link AIGovernancePipeline} before delegating provider resilience to {@link FailoverManager}.
 */
@Service
public class DefaultAIGateway implements AIGateway {

  private static final Logger log = LoggerFactory.getLogger(DefaultAIGateway.class);

  private final PromptRegistry promptRegistry;
  private final PromptRenderer promptRenderer;
  private final FailoverManager failoverManager;
  private final AIGovernancePipeline governancePipeline;

  /**
   * Constructs the gateway with required dependencies.
   *
   * @param promptRegistry prompt template registry
   * @param promptRenderer template renderer
   * @param failoverManager failover and resilience manager
   * @param governancePipeline governance execution pipeline
   */
  public DefaultAIGateway(
      PromptRegistry promptRegistry,
      PromptRenderer promptRenderer,
      FailoverManager failoverManager,
      AIGovernancePipeline governancePipeline) {
    this.promptRegistry = promptRegistry;
    this.promptRenderer = promptRenderer;
    this.failoverManager = failoverManager;
    this.governancePipeline = governancePipeline;
  }

  @Override
  public AIResponse execute(AIRequest request, AIExecutionContext context) {
    if (request == null || request.promptId() == null) {
      throw new AIConfigurationException(
          "Invalid AIRequest: promptId is required", "AI_INVALID_REQUEST");
    }

    AIExecutionContext execContext = (context != null) ? context : AIExecutionContext.empty();

    // 1. Resolve prompt configuration
    PromptDescriptor descriptor = promptRegistry.getPrompt(request.promptId());

    // 2. Render prompt template with context variables
    String renderedPrompt = promptRenderer.render(descriptor.template(), request.variables());

    // 3. Combine required capabilities
    Set<AICapability> requiredCapabilities = new HashSet<>(descriptor.capabilities());
    if (request.requiredCapabilities() != null) {
      requiredCapabilities.addAll(request.requiredCapabilities());
    }

    AIRequest routedRequest =
        new AIRequest(
            request.promptId(),
            request.variables(),
            request.imageUrl(),
            request.cacheBypass(),
            requiredCapabilities,
            request.requestedProvider());

    log.info(
        "DefaultAIGateway executing promptId={}, tenantId={}, correlationId={}",
        request.promptId(),
        execContext.tenantId(),
        execContext.correlationId());

    // 4. Delegate to governance pipeline before failover manager execution
    AIResponse rawResponse =
        governancePipeline.execute(
            routedRequest,
            renderedPrompt,
            descriptor,
            execContext,
            () -> failoverManager.executeWithFailover(routedRequest, renderedPrompt, descriptor));

    // 5. Normalize response tracking fields
    return normalizeResponse(rawResponse, descriptor);
  }

  private AIResponse normalizeResponse(AIResponse rawResponse, PromptDescriptor descriptor) {
    return new AIResponse(
        rawResponse.content(),
        descriptor.promptId(),
        rawResponse.provider(),
        rawResponse.model(),
        rawResponse.promptTokens(),
        rawResponse.completionTokens(),
        rawResponse.finishReason());
  }
}
