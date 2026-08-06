package app.vetra.ai.gateway.governance;

import app.vetra.ai.config.AIGatewayProperties;
import app.vetra.ai.config.GovernanceProperties;
import app.vetra.ai.exception.AIBudgetExceededException;
import app.vetra.ai.model.AIExecutionContext;
import app.vetra.ai.model.AIRequest;
import app.vetra.ai.model.AIResponse;
import app.vetra.ai.prompt.PromptDescriptor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * In-memory token limit validation, enforcement, and cost accounting manager. Performs validation
 * prior to execution and updates consumption counters post-execution without persistence or DB side-effects.
 */
@Component
public class AIBudgetManager {

  private static final Logger log = LoggerFactory.getLogger(AIBudgetManager.class);

  private final AIGatewayProperties properties;
  private final Map<String, AtomicLong> tenantDailyTokens = new ConcurrentHashMap<>();

  /**
   * Constructs AIBudgetManager with gateway configuration properties.
   *
   * @param properties gateway configuration properties
   */
  public AIBudgetManager(AIGatewayProperties properties) {
    this.properties = properties;
  }

  /**
   * Pre-execution budget validation.
   *
   * @param request incoming AI request
   * @param renderedPrompt rendered prompt template
   * @param descriptor prompt descriptor
   * @param context execution context
   * @throws AIBudgetExceededException if estimated tokens push tenant over daily budget
   */
  public void checkBudget(
      AIRequest request,
      String renderedPrompt,
      PromptDescriptor descriptor,
      AIExecutionContext context) {

    GovernanceProperties.BudgetConfig budgetConfig = properties.getGovernance().getBudget();

    if (!properties.getGovernance().isEnabled() || !budgetConfig.isEnabled()) {
      return;
    }

    String tenantId = context.tenantId();
    Long dailyLimit = budgetConfig.getTenantDailyTokenLimit().get(tenantId);

    if (dailyLimit != null && dailyLimit > 0) {
      long currentUsage = tenantDailyTokens.computeIfAbsent(tenantId, k -> new AtomicLong(0)).get();
      int estimatedTokens = renderedPrompt != null ? renderedPrompt.length() / 4 : 0;

      if (currentUsage + estimatedTokens > dailyLimit) {
        log.warn(
            "AIBudgetExceeded: Tenant '{}' current usage {} + estimated {} exceeds daily limit {}",
            tenantId,
            currentUsage,
            estimatedTokens,
            dailyLimit);
        throw new AIBudgetExceededException(
            "Budget exceeded: Tenant '" + tenantId + "' daily token limit reached",
            "governance.budget");
      }
    }
  }

  /**
   * Post-execution accounting of consumed tokens and estimated cost calculation.
   *
   * @param response normalized response from provider
   * @param context execution context
   * @return estimated cost in USD for this inference call
   */
  public double recordConsumption(AIResponse response, AIExecutionContext context) {
    if (response == null) {
      return 0.0;
    }

    String tenantId = context.tenantId();
    int tokensUsed = response.totalTokens();

    tenantDailyTokens
        .computeIfAbsent(tenantId, k -> new AtomicLong(0))
        .addAndGet(tokensUsed);

    GovernanceProperties.BudgetConfig budgetConfig = properties.getGovernance().getBudget();

    Double costPer1k =
        budgetConfig.getCostPer1kTokens().getOrDefault(response.provider().toLowerCase(), 0.002);

    double estimatedCost = (tokensUsed / 1000.0) * costPer1k;

    log.debug(
        "AIBudgetAccounting: Tenant '{}' consumed {} tokens (~${}) via provider '{}'",
        tenantId,
        tokensUsed,
        String.format("%.5f", estimatedCost),
        response.provider());

    return estimatedCost;
  }

  /** Resets in-memory budget counters (useful for testing or daily reset jobs). */
  public void resetUsage() {
    tenantDailyTokens.clear();
  }

  /**
   * Retrieves current accumulated daily token usage for a tenant.
   *
   * @param tenantId tenant identifier
   * @return token count
   */
  public long getTenantUsage(String tenantId) {
    AtomicLong counter = tenantDailyTokens.get(tenantId);
    return counter != null ? counter.get() : 0L;
  }
}
