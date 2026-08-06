package app.vetra.ai.gateway.governance;

import app.vetra.ai.config.AIGatewayProperties;
import app.vetra.ai.config.GovernanceProperties;
import app.vetra.ai.exception.AIPolicyViolationException;
import app.vetra.ai.model.AIExecutionContext;
import app.vetra.ai.model.AIRequest;
import app.vetra.ai.prompt.PromptDescriptor;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Enterprise Policy Engine evaluating configuration-driven restrictions (e.g. tenant-allowed providers,
 * tenant-allowed models, max token bounds) before request execution.
 */
@Component
public class AIPolicyEngine {

  private static final Logger log = LoggerFactory.getLogger(AIPolicyEngine.class);

  private final AIGatewayProperties properties;

  /**
   * Constructs AIPolicyEngine with gateway configuration properties.
   *
   * @param properties gateway configuration properties
   */
  public AIPolicyEngine(AIGatewayProperties properties) {
    this.properties = properties;
  }

  /**
   * Evaluates enterprise policy rules for the request.
   *
   * @param request incoming AIRequest
   * @param renderedPrompt rendered prompt template
   * @param descriptor prompt descriptor
   * @param context execution context
   * @throws AIPolicyViolationException if policy restrictions are violated
   */
  public void evaluate(
      AIRequest request,
      String renderedPrompt,
      PromptDescriptor descriptor,
      AIExecutionContext context) {

    GovernanceProperties.PolicyConfig policyConfig = properties.getGovernance().getPolicy();

    if (!properties.getGovernance().isEnabled() || !policyConfig.isEnabled()) {
      return;
    }

    log.debug(
        "Evaluating AIPolicyEngine for promptId={}, tenantId={}",
        descriptor.promptId(),
        context.tenantId());

    // 1. Tenant Provider Restrictions
    if (request.requestedProvider() != null) {
      String reqProviderStr = request.requestedProvider().name().toLowerCase();
      List<String> allowedProviders =
          policyConfig.getTenantAllowedProviders().get(context.tenantId());
      if (allowedProviders != null
          && !allowedProviders.isEmpty()
          && allowedProviders.stream().noneMatch(p -> p.equalsIgnoreCase(reqProviderStr))) {
        log.warn(
            "AIPolicyViolation: Provider '{}' is not allowed for tenantId '{}'",
            reqProviderStr,
            context.tenantId());
        throw new AIPolicyViolationException(
            "Policy violation: Provider '" + reqProviderStr + "' is not permitted for tenant",
            "governance.policy");
      }
    }

    // 2. Token Upper Bound Check
    if (renderedPrompt != null) {
      int estimatedTokens = renderedPrompt.length() / 4; // approximate token count
      if (estimatedTokens > policyConfig.getMaxPromptTokens()) {
        log.warn(
            "AIPolicyViolation: Rendered prompt length estimated at {} tokens exceeds max allowed {}",
            estimatedTokens,
            policyConfig.getMaxPromptTokens());
        throw new AIPolicyViolationException(
            "Policy violation: Prompt token estimate exceeds maximum limit of "
                + policyConfig.getMaxPromptTokens(),
            "governance.policy");
      }
    }
  }
}
