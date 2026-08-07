package app.vetra.ai.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AIMetricsCollectorTest {

  private MeterRegistry meterRegistry;
  private AIMetricsCollector collector;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    collector = new AIMetricsCollector(meterRegistry);
  }

  @Test
  void testRecordRequest_success() {
    collector.recordRequest("gemini", "gemini-1.5-flash", "test.v1", "v1", "success", 100_000_000L);

    double reqTotal = meterRegistry.get(AIDashboardMetadata.METRIC_REQUESTS_TOTAL).counter().count();
    assertEquals(1.0, reqTotal);

    double provTotal =
        meterRegistry
            .get(AIDashboardMetadata.METRIC_PROVIDER_REQUESTS_TOTAL)
            .tag(AIDashboardMetadata.TAG_PROVIDER, "gemini")
            .tag(AIDashboardMetadata.TAG_STATUS, "success")
            .counter()
            .count();
    assertEquals(1.0, provTotal);

    double modelTotal =
        meterRegistry
            .get(AIDashboardMetadata.METRIC_MODEL_REQUESTS_TOTAL)
            .tag(AIDashboardMetadata.TAG_MODEL, "gemini-1.5-flash")
            .counter()
            .count();
    assertEquals(1.0, modelTotal);
  }

  @Test
  void testRecordGovernanceRejection() {
    collector.recordGovernanceRejection("safety", "test.v1", "v1");

    double count =
        meterRegistry
            .get(AIDashboardMetadata.METRIC_GOVERNANCE_REJECTIONS_TOTAL)
            .tag(AIDashboardMetadata.TAG_GOVERNANCE_TYPE, "safety")
            .counter()
            .count();
    assertEquals(1.0, count);
  }

  @Test
  void testRecordTokenUsageAndCost() {
    collector.recordTokenUsage("gemini", "gemini-1.5-flash", 100, 50);
    collector.recordCost("gemini", "gemini-1.5-flash", 0.0025);

    double promptTokens =
        meterRegistry
            .get(AIDashboardMetadata.METRIC_PROMPT_TOKENS_TOTAL)
            .tag(AIDashboardMetadata.TAG_PROVIDER, "gemini")
            .counter()
            .count();
    assertEquals(100.0, promptTokens);

    double completionTokens =
        meterRegistry
            .get(AIDashboardMetadata.METRIC_COMPLETION_TOKENS_TOTAL)
            .tag(AIDashboardMetadata.TAG_PROVIDER, "gemini")
            .counter()
            .count();
    assertEquals(50.0, completionTokens);

    double cost =
        meterRegistry
            .get(AIDashboardMetadata.METRIC_ESTIMATED_COST_TOTAL)
            .tag(AIDashboardMetadata.TAG_PROVIDER, "gemini")
            .counter()
            .count();
    assertEquals(0.0025, cost, 0.0001);
  }

  @Test
  void testNullMeterRegistrySafety() {
    AIMetricsCollector safeCollector = new AIMetricsCollector(null);
    // Should not throw NullPointerException
    safeCollector.recordRequest("noop", "noop-v1", "test.v1", "v1", "success", 500L);
    safeCollector.recordGovernanceRejection("policy", "test.v1", "v1");
    safeCollector.recordTokenUsage("noop", "noop-v1", 10, 10);
    safeCollector.recordCost("noop", "noop-v1", 0.001);
  }
}
