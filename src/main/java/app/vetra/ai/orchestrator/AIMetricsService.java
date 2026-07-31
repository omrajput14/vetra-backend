package app.vetra.ai.orchestrator;

import app.vetra.ai.entity.AIProviderType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

/**
 * In-memory thread-safe service tracking latency, success rates, and availability metrics per AI provider.
 */
@Service
public class AIMetricsService {

  private final AtomicLong totalRequests = new AtomicLong(0);
  private final AtomicLong successfulRequests = new AtomicLong(0);
  private final AtomicLong failedRequests = new AtomicLong(0);

  private final Map<AIProviderType, AtomicLong> providerTotalLatency = new ConcurrentHashMap<>();
  private final Map<AIProviderType, AtomicLong> providerSuccessCount = new ConcurrentHashMap<>();
  private final Map<AIProviderType, AtomicLong> providerFailureCount = new ConcurrentHashMap<>();

  /**
   * Records a successful AI inference request.
   *
   * @param provider provider type
   * @param latencyMs latency duration in milliseconds
   */
  public void recordSuccess(AIProviderType provider, long latencyMs) {
    totalRequests.incrementAndGet();
    successfulRequests.incrementAndGet();

    providerTotalLatency.computeIfAbsent(provider, p -> new AtomicLong(0)).addAndGet(latencyMs);
    providerSuccessCount.computeIfAbsent(provider, p -> new AtomicLong(0)).incrementAndGet();
  }

  /**
   * Records a failed AI inference request.
   *
   * @param provider provider type
   * @param latencyMs latency duration in milliseconds
   */
  public void recordFailure(AIProviderType provider, long latencyMs) {
    totalRequests.incrementAndGet();
    failedRequests.incrementAndGet();

    providerTotalLatency.computeIfAbsent(provider, p -> new AtomicLong(0)).addAndGet(latencyMs);
    providerFailureCount.computeIfAbsent(provider, p -> new AtomicLong(0)).incrementAndGet();
  }

  public long getTotalRequests() {
    return totalRequests.get();
  }

  public long getSuccessfulRequests() {
    return successfulRequests.get();
  }

  public long getFailedRequests() {
    return failedRequests.get();
  }

  /**
   * Calculates average latency in milliseconds for a provider.
   *
   * @param provider provider type
   * @return average latency ms, or 0 if no requests recorded
   */
  public double getAverageLatencyMs(AIProviderType provider) {
    long totalLat = providerTotalLatency.getOrDefault(provider, new AtomicLong(0)).get();
    long success = providerSuccessCount.getOrDefault(provider, new AtomicLong(0)).get();
    long failure = providerFailureCount.getOrDefault(provider, new AtomicLong(0)).get();
    long count = success + failure;

    return count > 0 ? (double) totalLat / count : 0.0;
  }
}
