package app.vetra.ai.gateway.governance;

import app.vetra.ai.model.AIExecutionContext;
import app.vetra.ai.model.AIRequest;
import app.vetra.ai.model.AIResponse;
import app.vetra.ai.prompt.PromptDescriptor;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link AIGovernancePipeline}. Orchestrates pre-execution safety,
 * policy, and budget checks before delegating to downstream provider execution, followed by
 * post-execution consumption accounting and audit logging.
 */
@Service
public class DefaultAIGovernancePipeline implements AIGovernancePipeline {

  private static final Logger log = LoggerFactory.getLogger(DefaultAIGovernancePipeline.class);

  private final AISafetyFilter safetyFilter;
  private final AIPolicyEngine policyEngine;
  private final AIBudgetManager budgetManager;
  private final AIAuditService auditService;

  /**
   * Constructs the governance pipeline with required governance services.
   *
   * @param safetyFilter safety filter component
   * @param policyEngine policy engine component
   * @param budgetManager budget manager component
   * @param auditService audit service component
   */
  public DefaultAIGovernancePipeline(
      AISafetyFilter safetyFilter,
      AIPolicyEngine policyEngine,
      AIBudgetManager budgetManager,
      AIAuditService auditService) {
    this.safetyFilter = safetyFilter;
    this.policyEngine = policyEngine;
    this.budgetManager = budgetManager;
    this.auditService = auditService;
  }

  @Override
  public AIResponse execute(
      AIRequest request,
      String renderedPrompt,
      PromptDescriptor descriptor,
      AIExecutionContext context,
      Supplier<AIResponse> executionChain) {

    // 1. Safety Filter Evaluation
    safetyFilter.evaluate(request, renderedPrompt, descriptor, context);

    // 2. Enterprise Policy Engine Evaluation
    policyEngine.evaluate(request, renderedPrompt, descriptor, context);

    // 3. Pre-flight Budget Limit Validation
    budgetManager.checkBudget(request, renderedPrompt, descriptor, context);

    long startTime = System.currentTimeMillis();
    AIResponse response = null;
    double estimatedCost = 0.0;

    try {
      // 4. Delegate to downstream execution (FailoverManager & Providers)
      response = executionChain.get();
      long latencyMs = System.currentTimeMillis() - startTime;

      // 5. Post-execution Budget Consumption & Accounting
      estimatedCost = budgetManager.recordConsumption(response, context);

      // 6. Record Audit Telemetry
      auditService.recordAuditEvent(
          request, renderedPrompt, descriptor, response, context, estimatedCost, latencyMs, null);

      return response;

    } catch (RuntimeException ex) {
      long latencyMs = System.currentTimeMillis() - startTime;
      auditService.recordAuditEvent(
          request, renderedPrompt, descriptor, null, context, 0.0, latencyMs, ex);
      throw ex;
    }
  }
}
