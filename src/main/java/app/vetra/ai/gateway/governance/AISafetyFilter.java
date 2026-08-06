package app.vetra.ai.gateway.governance;

import app.vetra.ai.config.AIGatewayProperties;
import app.vetra.ai.config.GovernanceProperties;
import app.vetra.ai.exception.AISafetyViolationException;
import app.vetra.ai.model.AIExecutionContext;
import app.vetra.ai.model.AIRequest;
import app.vetra.ai.prompt.PromptDescriptor;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Validates requests and rendered prompts against enterprise safety guidelines and keyword rules.
 * Fast-fails execution by throwing {@link AISafetyViolationException} before any provider call.
 */
@Component
public class AISafetyFilter {

  private static final Logger log = LoggerFactory.getLogger(AISafetyFilter.class);

  private final AIGatewayProperties properties;

  /**
   * Constructs AISafetyFilter with gateway configuration properties.
   *
   * @param properties gateway configuration properties
   */
  public AISafetyFilter(AIGatewayProperties properties) {
    this.properties = properties;
  }

  /**
   * Evaluates safety rules for the given request and rendered prompt.
   *
   * @param request the incoming AIRequest
   * @param renderedPrompt the rendered prompt template
   * @param descriptor the prompt descriptor
   * @param context the execution context
   * @throws AISafetyViolationException if safety rules are violated
   */
  public void evaluate(
      AIRequest request,
      String renderedPrompt,
      PromptDescriptor descriptor,
      AIExecutionContext context) {

    GovernanceProperties.SafetyConfig safetyConfig = properties.getGovernance().getSafety();

    if (!properties.getGovernance().isEnabled() || !safetyConfig.isEnabled()) {
      return;
    }

    log.debug(
        "Evaluating AISafetyFilter for promptId={}, tenantId={}, safetyLevel={}",
        descriptor.promptId(),
        context.tenantId(),
        descriptor.safetyLevel());

    // Check prompt text against blocked keywords / patterns
    List<String> blocked = safetyConfig.getBlockedKeywords();
    if (blocked != null && renderedPrompt != null) {
      String lowerPrompt = renderedPrompt.toLowerCase();
      for (String keyword : blocked) {
        if (keyword != null && !keyword.isBlank() && lowerPrompt.contains(keyword.toLowerCase())) {
          log.warn(
              "AISafetyViolation: Prompt contained blocked keyword '{}' for promptId={}",
              keyword,
              descriptor.promptId());
          throw new AISafetyViolationException(
              "Safety violation: prompt contains prohibited content", "governance.safety");
        }
      }
    }
  }
}
