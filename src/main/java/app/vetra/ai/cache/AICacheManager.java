package app.vetra.ai.cache;

import app.vetra.ai.config.AIGatewayProperties;
import app.vetra.ai.config.CacheProperties;
import app.vetra.ai.config.GovernanceProperties.BudgetConfig;
import app.vetra.ai.model.AIResponse;
import app.vetra.infrastructure.cache.CacheNames;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.tracing.Tracer;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * Enterprise AI cache manager providing Redis-backed response caching, single-flight cache stampede
 * protection, graceful Redis fault-tolerance, and cost/token savings observability.
 */
@Component
public class AICacheManager implements AICacheWarmupService {

  private static final Logger log = LoggerFactory.getLogger(AICacheManager.class);

  private final CacheManager cacheManager;
  private final AIGatewayProperties gatewayProperties;
  private final MeterRegistry meterRegistry;
  private final Tracer tracer;

  // Single-flight stampede protection map
  private final ConcurrentHashMap<String, CompletableFuture<AIResponse>> inFlightRequests =
      new ConcurrentHashMap<>();

  /**
   * Constructs AICacheManager with required and optional dependencies.
   *
   * @param cacheManager Spring CacheManager instance
   * @param gatewayProperties AI Gateway configuration properties
   * @param meterRegistry Micrometer metrics registry (optional)
   * @param tracer OpenTelemetry tracer (optional)
   */
  public AICacheManager(
      CacheManager cacheManager,
      AIGatewayProperties gatewayProperties,
      @Autowired(required = false) MeterRegistry meterRegistry,
      @Autowired(required = false) Tracer tracer) {
    this.cacheManager = cacheManager;
    this.gatewayProperties = gatewayProperties;
    this.meterRegistry = meterRegistry;
    this.tracer = tracer;
  }

  /**
   * Performs cache lookup. If hit, returns cached {@link AIResponse}. If miss or bypass, executes
   * provider supplier with single-flight stampede protection and caches successful response.
   *
   * @param cacheKey deterministic cache key
   * @param promptVersion prompt template version
   * @param bypass true if cache bypass was requested
   * @param executionSupplier supplier calling downstream provider execution
   * @return normalized AIResponse
   */
  public AIResponse getOrCompute(
      String cacheKey,
      String promptVersion,
      boolean bypass,
      Supplier<AIResponse> executionSupplier) {

    CacheProperties cacheProps =
        gatewayProperties.getCache() != null
            ? gatewayProperties.getCache()
            : new CacheProperties();

    if (bypass || !cacheProps.isEnabled()) {
      recordMetric("ai_cache_lookup_total", "result", bypass ? "bypass" : "disabled");
      recordSpanEvent("ai.cache.bypass", "cacheKey", cacheKey);
      return executionSupplier.get();
    }

    long startTime = System.nanoTime();

    // 1. Try Cache Lookup
    Optional<CachedAIResponse> cachedOpt = getFromCache(cacheKey);
    if (cachedOpt.isPresent()) {
      long durationNanos = System.nanoTime() - startTime;
      recordTimer("ai_cache_lookup_duration", durationNanos, "result", "hit");
      recordMetric("ai_cache_lookup_total", "result", "hit");
      recordSpanEvent("ai.cache.hit", "cacheKey", cacheKey);

      AIResponse cachedResponse = cachedOpt.get().response();

      // Record saved tokens and estimated cost saved
      recordTokensSaved(cachedResponse.totalTokens());
      recordCostSaved(estimateCost(cachedResponse));

      log.info(
          "AICache HIT cacheKey={} provider={} model={} totalTokensSaved={}",
          cacheKey,
          cachedResponse.provider(),
          cachedResponse.model(),
          cachedResponse.totalTokens());

      return cachedResponse;
    }

    recordMetric("ai_cache_lookup_total", "result", "miss");
    recordSpanEvent("ai.cache.miss", "cacheKey", cacheKey);

    // 2. Cache Stampede Protection (Single-Flight Pattern)
    boolean[] isLeader = new boolean[] {false};
    CompletableFuture<AIResponse> future =
        inFlightRequests.computeIfAbsent(
            cacheKey,
            k -> {
              isLeader[0] = true;
              return new CompletableFuture<>();
            });

    if (!isLeader[0]) {
      // Waiter thread in single-flight lock
      log.info("AICache STAMPEDE_WAIT in-flight request for cacheKey={}", cacheKey);
      recordSpanEvent("ai.cache.stampede_wait", "cacheKey", cacheKey);
      try {
        long lockTimeoutSec = cacheProps.getStampedeLockTimeout().getSeconds();
        AIResponse response = future.get(lockTimeoutSec, TimeUnit.SECONDS);

        long durationNanos = System.nanoTime() - startTime;
        recordTimer("ai_cache_lookup_duration", durationNanos, "result", "stampede_hit");
        recordMetric("ai_cache_lookup_total", "result", "stampede_hit");

        recordTokensSaved(response.totalTokens());
        recordCostSaved(estimateCost(response));

        return response;
      } catch (Exception ex) {
        log.warn("AICache stampede wait timed out/failed for cacheKey={}. Falling back to fresh execution.", cacheKey, ex);
        return executionSupplier.get();
      }
    }

    // Leader thread: execute provider, populate cache, complete future
    try {
      AIResponse freshResponse = executionSupplier.get();
      if (freshResponse != null && freshResponse.content() != null && !freshResponse.content().isBlank()) {
        putInCache(cacheKey, CachedAIResponse.of(freshResponse, promptVersion, cacheKey));
      }
      future.complete(freshResponse);
      long durationNanos = System.nanoTime() - startTime;
      recordTimer("ai_cache_lookup_duration", durationNanos, "result", "miss_executed");
      return freshResponse;

    } catch (Throwable t) {
      future.completeExceptionally(t);
      if (t instanceof RuntimeException re) {
        throw re;
      }
      throw new IllegalStateException("AI execution failed", t);
    } finally {
      inFlightRequests.remove(cacheKey);
    }
  }

  /**
   * Retrieves entry from Spring Cache.
   *
   * @param cacheKey deterministic key
   * @return optional CachedAIResponse
   */
  public Optional<CachedAIResponse> getFromCache(String cacheKey) {
    try {
      Cache cache = cacheManager.getCache(CacheNames.AI_DIAGNOSIS);
      if (cache != null) {
        Cache.ValueWrapper wrapper = cache.get(cacheKey);
        if (wrapper != null && wrapper.get() instanceof CachedAIResponse cached) {
          return Optional.of(cached);
        }
      }
    } catch (Exception ex) {
      log.warn("AICache GET error on key={}. Falling back to miss.", cacheKey, ex);
      recordMetric("ai_cache_lookup_total", "result", "error");
    }
    return Optional.empty();
  }

  /**
   * Places entry into Spring Cache.
   *
   * @param cacheKey deterministic key
   * @param entry cached response record
   */
  public void putInCache(String cacheKey, CachedAIResponse entry) {
    try {
      Cache cache = cacheManager.getCache(CacheNames.AI_DIAGNOSIS);
      if (cache != null) {
        cache.put(cacheKey, entry);
        recordMetric("ai_cache_write_total", "status", "success");
      }
    } catch (Exception ex) {
      log.warn("AICache PUT error on key={}. Continuing execution.", cacheKey, ex);
      recordMetric("ai_cache_write_total", "status", "error");
    }
  }

  /**
   * Evicts entry from Spring Cache.
   *
   * @param cacheKey deterministic key
   */
  public void evict(String cacheKey) {
    try {
      Cache cache = cacheManager.getCache(CacheNames.AI_DIAGNOSIS);
      if (cache != null) {
        cache.evict(cacheKey);
        recordMetric("ai_cache_eviction_total", "status", "success");
      }
    } catch (Exception ex) {
      log.warn("AICache EVICT error on key={}", cacheKey, ex);
      recordMetric("ai_cache_eviction_total", "status", "error");
    }
  }

  @Override
  public void warmupCache(String cacheKey, AIResponse response, String promptVersion, Duration ttl) {
    if (cacheKey != null && response != null) {
      putInCache(cacheKey, CachedAIResponse.of(response, promptVersion, cacheKey));
      log.info("AICache WARMUP preloaded cacheKey={}", cacheKey);
    }
  }

  // ── Observability Helpers ──────────────────────────────────────────────────

  private void recordTokensSaved(int tokens) {
    if (meterRegistry != null && tokens > 0) {
      Counter.builder("ai_token_saved_total")
          .description("Total AI prompt and completion tokens saved via cache hits")
          .register(meterRegistry)
          .increment(tokens);
    }
  }

  private void recordCostSaved(double costUSD) {
    if (meterRegistry != null && costUSD > 0.0) {
      Counter.builder("ai_cost_saved_total")
          .description("Total estimated USD cost saved via cache hits")
          .register(meterRegistry)
          .increment(costUSD);
    }
  }

  private double estimateCost(AIResponse response) {
    if (response == null || gatewayProperties.getGovernance() == null) {
      return 0.0;
    }
    BudgetConfig budget = gatewayProperties.getGovernance().getBudget();
    if (budget == null || budget.getCostPer1kTokens() == null) {
      return 0.0;
    }
    Double ratePer1k = budget.getCostPer1kTokens().getOrDefault(response.provider().toLowerCase(), 0.0015);
    return (double) response.totalTokens() / 1000.0 * ratePer1k;
  }

  private void recordMetric(String name, String... tags) {
    if (meterRegistry != null) {
      Counter.builder(name).tags(tags).register(meterRegistry).increment();
    }
  }

  private void recordTimer(String name, long durationNanos, String... tags) {
    if (meterRegistry != null) {
      Timer.builder(name)
          .tags(tags)
          .register(meterRegistry)
          .record(durationNanos, TimeUnit.NANOSECONDS);
    }
  }

  private void recordSpanEvent(String eventName, String... keyValues) {
    if (tracer != null && tracer.currentSpan() != null) {
      tracer.currentSpan().event(eventName);
    }
  }
}
