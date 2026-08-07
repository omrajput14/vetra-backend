package app.vetra.ai.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.vetra.ai.config.AIGatewayProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AIAnalyticsServiceTest {

  private MeterRegistry meterRegistry;
  private AIGatewayProperties gatewayProperties;
  private AIAnalyticsService analyticsService;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    gatewayProperties = AIGatewayProperties.builder().build();
    analyticsService = new AIAnalyticsService(meterRegistry, gatewayProperties);
  }

  @Test
  void testCalculateCacheHitRatio() {
    Counter.builder("ai_cache_lookup_total").tag("result", "hit").register(meterRegistry).increment(8);
    Counter.builder("ai_cache_lookup_total").tag("result", "miss").register(meterRegistry).increment(2);

    double ratio = analyticsService.calculateCacheHitRatio();
    assertEquals(0.8, ratio, 0.001);
  }

  @Test
  void testGetTotalTokensAndCostSaved() {
    Counter.builder("ai_token_saved_total").register(meterRegistry).increment(500);
    Counter.builder("ai_cost_saved_total").register(meterRegistry).increment(0.0075);

    assertEquals(500L, analyticsService.getTotalTokensSaved());
    assertEquals(0.0075, analyticsService.getTotalCostSavedUSD(), 0.0001);
  }

  @Test
  void testGetProviderRequestBreakdown() {
    Counter.builder(AIDashboardMetadata.METRIC_PROVIDER_REQUESTS_TOTAL)
        .tag(AIDashboardMetadata.TAG_PROVIDER, "gemini")
        .tag(AIDashboardMetadata.TAG_STATUS, "success")
        .register(meterRegistry)
        .increment(5);

    Counter.builder(AIDashboardMetadata.METRIC_PROVIDER_REQUESTS_TOTAL)
        .tag(AIDashboardMetadata.TAG_PROVIDER, "noop")
        .tag(AIDashboardMetadata.TAG_STATUS, "success")
        .register(meterRegistry)
        .increment(2);

    Map<String, Long> breakdown = analyticsService.getProviderRequestBreakdown();
    assertEquals(5L, breakdown.get("gemini"));
    assertEquals(2L, breakdown.get("noop"));
  }

  @Test
  void testCalculateCostForTokens() {
    double cost = analyticsService.calculateCostForTokens("gemini", 2000);
    assertEquals(0.003, cost, 0.0001); // 2k * $0.0015 / 1k = 0.003
  }
}
