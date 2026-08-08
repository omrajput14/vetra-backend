package app.vetra.ai.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Consolidated operational metrics collector for the AI Gateway subsystem.
 *
 * <p>Strictly enforces low-cardinality tags (provider, model, promptId, promptVersion, status) and
 * records requests, latency histograms, governance rejections, token consumption, and cost estimates.
 */
@Service
public class AIMetricsCollector {

  private static final Logger log = LoggerFactory.getLogger(AIMetricsCollector.class);

  private final MeterRegistry meterRegistry;

  /**
   * Constructs AIMetricsCollector.
   *
   * @param meterRegistry Micrometer meter registry (optional-injected for test safety)
   */
  public AIMetricsCollector(@Autowired(required = false) MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  /**
   * Records an AI request execution attempt and its latency.
   *
   * @param provider provider name
   * @param model model alias/ID
   * @param promptId prompt template ID
   * @param promptVersion prompt template version
   * @param status execution status (success/failure)
   * @param latencyNanos execution duration in nanoseconds
   */
  public void recordRequest(
      String provider,
      String model,
      String promptId,
      String promptVersion,
      String status,
      long latencyNanos) {

    String p = normalizeTagValue(provider, "UNKNOWN");
    String m = normalizeTagValue(model, "UNKNOWN");
    String pid = normalizeTagValue(promptId, "UNKNOWN");
    String pv = normalizeTagValue(promptVersion, "UNKNOWN");
    String st = normalizeTagValue(status, "UNKNOWN");

    if (meterRegistry != null) {
      Counter.builder(AIDashboardMetadata.METRIC_REQUESTS_TOTAL)
          .description("Total AI gateway requests by status")
          .tag(AIDashboardMetadata.TAG_STATUS, st)
          .register(meterRegistry)
          .increment();

      Counter.builder(AIDashboardMetadata.METRIC_PROVIDER_REQUESTS_TOTAL)
          .description("Total requests by provider and status")
          .tag(AIDashboardMetadata.TAG_PROVIDER, p)
          .tag(AIDashboardMetadata.TAG_STATUS, st)
          .register(meterRegistry)
          .increment();

      Counter.builder(AIDashboardMetadata.METRIC_MODEL_REQUESTS_TOTAL)
          .description("Total requests by model alias and status")
          .tag(AIDashboardMetadata.TAG_MODEL, m)
          .tag(AIDashboardMetadata.TAG_STATUS, st)
          .register(meterRegistry)
          .increment();

      Timer.builder(AIDashboardMetadata.METRIC_REQUEST_DURATION)
          .description("AI request duration SLA timer")
          .tag(AIDashboardMetadata.TAG_PROVIDER, p)
          .tag(AIDashboardMetadata.TAG_PROMPT_ID, pid)
          .tag(AIDashboardMetadata.TAG_STATUS, st)
          .publishPercentiles(0.5, 0.95, 0.99)
          .register(meterRegistry)
          .record(latencyNanos, TimeUnit.NANOSECONDS);

      Timer.builder(AIDashboardMetadata.METRIC_PROVIDER_LATENCY)
          .description("AI provider execution latency SLA timer")
          .tag(AIDashboardMetadata.TAG_PROVIDER, p)
          .tag(AIDashboardMetadata.TAG_MODEL, m)
          .publishPercentiles(0.5, 0.95, 0.99)
          .register(meterRegistry)
          .record(latencyNanos, TimeUnit.NANOSECONDS);
    }
  }

  /**
   * Records an agent execution attempt, capability, result, and latency.
   *
   * @param agent agent name
   * @param capability capability executed
   * @param result result status (SUCCESS, CACHE_HIT, FAILED_PROVIDER, FAILED_GOVERNANCE, etc.)
   * @param latencyNanos duration in nanoseconds
   */
  public void recordAgentExecution(
      String agent, String capability, String result, long latencyNanos) {

    String ag = normalizeTagValue(agent, "UNKNOWN");
    String cap = normalizeTagValue(capability, "UNKNOWN");
    String res = normalizeTagValue(result, "UNKNOWN");

    if (meterRegistry != null) {
      Counter.builder(AIDashboardMetadata.METRIC_AGENT_REQUESTS_TOTAL)
          .description("Total AI agent requests by agent, capability, and result")
          .tag(AIDashboardMetadata.TAG_AGENT, ag)
          .tag(AIDashboardMetadata.TAG_CAPABILITY, cap)
          .tag(AIDashboardMetadata.TAG_RESULT, res)
          .register(meterRegistry)
          .increment();

      Timer.builder(AIDashboardMetadata.METRIC_AGENT_DURATION)
          .description("AI agent execution latency timer")
          .tag(AIDashboardMetadata.TAG_AGENT, ag)
          .tag(AIDashboardMetadata.TAG_CAPABILITY, cap)
          .publishPercentiles(0.5, 0.95, 0.99)
          .register(meterRegistry)
          .record(latencyNanos, TimeUnit.NANOSECONDS);
    }
  }

  /**
   * Records a governance check rejection.
   *
   * @param governanceType safety, policy, or budget
   * @param promptId prompt ID
   * @param promptVersion prompt version
   */
  public void recordGovernanceRejection(
      String governanceType, String promptId, String promptVersion) {

    String gt = normalizeTagValue(governanceType, "UNKNOWN");
    String pid = normalizeTagValue(promptId, "UNKNOWN");
    String pv = normalizeTagValue(promptVersion, "UNKNOWN");

    if (meterRegistry != null) {
      Counter.builder(AIDashboardMetadata.METRIC_GOVERNANCE_REJECTIONS_TOTAL)
          .description("Total AI governance rejections by type")
          .tag(AIDashboardMetadata.TAG_GOVERNANCE_TYPE, gt)
          .tag(AIDashboardMetadata.TAG_PROMPT_ID, pid)
          .tag(AIDashboardMetadata.TAG_PROMPT_VERSION, pv)
          .register(meterRegistry)
          .increment();
    }
  }

  /**
   * Records prompt and completion token consumption.
   *
   * @param provider provider name
   * @param model model alias
   * @param promptTokens input prompt token count
   * @param completionTokens output completion token count
   */
  public void recordTokenUsage(
      String provider, String model, int promptTokens, int completionTokens) {

    String p = normalizeTagValue(provider, "UNKNOWN");
    String m = normalizeTagValue(model, "UNKNOWN");

    if (meterRegistry != null) {
      if (promptTokens > 0) {
        Counter.builder(AIDashboardMetadata.METRIC_PROMPT_TOKENS_TOTAL)
            .description("Total AI prompt input tokens consumed")
            .tag(AIDashboardMetadata.TAG_PROVIDER, p)
            .tag(AIDashboardMetadata.TAG_MODEL, m)
            .register(meterRegistry)
            .increment(promptTokens);
      }
      if (completionTokens > 0) {
        Counter.builder(AIDashboardMetadata.METRIC_COMPLETION_TOKENS_TOTAL)
            .description("Total AI completion output tokens consumed")
            .tag(AIDashboardMetadata.TAG_PROVIDER, p)
            .tag(AIDashboardMetadata.TAG_MODEL, m)
            .register(meterRegistry)
            .increment(completionTokens);
      }
    }
  }

  /**
   * Records estimated inference cost in USD.
   *
   * @param provider provider name
   * @param model model alias
   * @param costUSD estimated cost in USD
   */
  public void recordCost(String provider, String model, double costUSD) {
    String p = normalizeTagValue(provider, "UNKNOWN");
    String m = normalizeTagValue(model, "UNKNOWN");

    if (meterRegistry != null && costUSD > 0.0) {
      Counter.builder(AIDashboardMetadata.METRIC_ESTIMATED_COST_TOTAL)
          .description("Total estimated AI spend in USD")
          .tag(AIDashboardMetadata.TAG_PROVIDER, p)
          .tag(AIDashboardMetadata.TAG_MODEL, m)
          .register(meterRegistry)
          .increment(costUSD);
    }
  }

  private String normalizeTagValue(String val, String fallback) {
    if (val == null || val.isBlank()) {
      return fallback;
    }
    return val.trim().toLowerCase();
  }
}
