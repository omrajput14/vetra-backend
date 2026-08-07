package app.vetra.ai.observability;

import app.vetra.ai.config.AIGatewayProperties;
import app.vetra.ai.config.GovernanceProperties.BudgetConfig;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Read-only operational analytics aggregation service for the AI subsystem.
 *
 * <p>Exposes capacity planning calculations, token spend summaries, SLA compliance ratios, and
 * cache efficiency statistics. Strictly read-only; never influences request execution, routing, or
 * governance decisions.
 */
@Service
public class AIAnalyticsService {

  private final MeterRegistry meterRegistry;
  private final AIGatewayProperties gatewayProperties;

  /**
   * Constructs AIAnalyticsService.
   *
   * @param meterRegistry Micrometer meter registry (optional)
   * @param gatewayProperties AI Gateway configuration properties
   */
  public AIAnalyticsService(
      @Autowired(required = false) MeterRegistry meterRegistry,
      AIGatewayProperties gatewayProperties) {
    this.meterRegistry = meterRegistry;
    this.gatewayProperties = gatewayProperties;
  }

  /**
   * Calculates overall AI Cache Hit Ratio (0.0 to 1.0).
   *
   * @return cache hit ratio percentage decimal
   */
  public double calculateCacheHitRatio() {
    if (meterRegistry == null) {
      return 0.0;
    }
    double hits = getCounterValue("ai_cache_lookup_total", "result", "hit");
    double stampedeHits = getCounterValue("ai_cache_lookup_total", "result", "stampede_hit");
    double misses = getCounterValue("ai_cache_lookup_total", "result", "miss");

    double totalLookups = hits + stampedeHits + misses;
    return totalLookups > 0 ? (hits + stampedeHits) / totalLookups : 0.0;
  }

  /**
   * Calculates total saved tokens via cache hits.
   *
   * @return total saved token count
   */
  public long getTotalTokensSaved() {
    if (meterRegistry == null) {
      return 0L;
    }
    return (long) getCounterValue("ai_token_saved_total");
  }

  /**
   * Calculates total cost saved in USD via cache hits.
   *
   * @return total cost saved in USD
   */
  public double getTotalCostSavedUSD() {
    if (meterRegistry == null) {
      return 0.0;
    }
    return getCounterValue("ai_cost_saved_total");
  }

  /**
   * Estimates total spend in USD across providers based on configured cost per 1k tokens.
   *
   * @return estimated total USD cost
   */
  public double getEstimatedTotalSpendUSD() {
    if (meterRegistry == null) {
      return 0.0;
    }
    return getCounterValue(AIDashboardMetadata.METRIC_ESTIMATED_COST_TOTAL);
  }

  /**
   * Returns a breakdown of request counts by provider.
   *
   * @return map of provider name to total request count
   */
  public Map<String, Long> getProviderRequestBreakdown() {
    Map<String, Long> summary = new HashMap<>();
    if (meterRegistry != null) {
      meterRegistry
          .find(AIDashboardMetadata.METRIC_PROVIDER_REQUESTS_TOTAL)
          .counters()
          .forEach(
              counter -> {
                String provider = counter.getId().getTag(AIDashboardMetadata.TAG_PROVIDER);
                if (provider != null) {
                  summary.merge(provider, (long) counter.count(), Long::sum);
                }
              });
    }
    return summary;
  }

  /**
   * Calculates estimated cost for a given token count and provider.
   *
   * @param provider provider name
   * @param totalTokens total prompt and completion tokens
   * @return estimated cost in USD
   */
  public double calculateCostForTokens(String provider, int totalTokens) {
    if (totalTokens <= 0 || provider == null || gatewayProperties.getGovernance() == null) {
      return 0.0;
    }
    BudgetConfig budget = gatewayProperties.getGovernance().getBudget();
    if (budget == null || budget.getCostPer1kTokens() == null) {
      return 0.0;
    }
    Double ratePer1k =
        budget.getCostPer1kTokens().getOrDefault(provider.toLowerCase(), 0.0015);
    return (double) totalTokens / 1000.0 * ratePer1k;
  }

  private double getCounterValue(String name, String... tags) {
    if (meterRegistry == null) {
      return 0.0;
    }
    Counter counter = meterRegistry.find(name).tags(tags).counter();
    return counter != null ? counter.count() : 0.0;
  }
}
