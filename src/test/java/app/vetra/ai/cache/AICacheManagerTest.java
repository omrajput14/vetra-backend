package app.vetra.ai.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.vetra.ai.config.AIGatewayProperties;
import app.vetra.ai.model.AIResponse;
import app.vetra.infrastructure.cache.CacheNames;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

class AICacheManagerTest {

  private CacheManager cacheManager;
  private AIGatewayProperties properties;
  private MeterRegistry meterRegistry;
  private AICacheManager aiCacheManager;

  @BeforeEach
  void setUp() {
    cacheManager = new ConcurrentMapCacheManager(CacheNames.AI_DIAGNOSIS);
    properties = AIGatewayProperties.builder().enabled(true).build();
    meterRegistry = new SimpleMeterRegistry();
    aiCacheManager = new AICacheManager(cacheManager, properties, meterRegistry, null);
  }

  @Test
  void testCacheMissThenHit() {
    String cacheKey = "vetra:ai:cache:testkey1";
    AIResponse mockResponse =
        new AIResponse("diagnosis content", "v1", "noop", "noop-v1", 50, 100, "stop");

    AtomicInteger providerExecutions = new AtomicInteger(0);
    Supplier<AIResponse> supplier =
        () -> {
          providerExecutions.incrementAndGet();
          return mockResponse;
        };

    // 1st Execution (Cache Miss)
    AIResponse resp1 = aiCacheManager.getOrCompute(cacheKey, "v1", false, supplier);
    assertEquals("diagnosis content", resp1.content());
    assertEquals(1, providerExecutions.get());

    // 2nd Execution (Cache Hit)
    AIResponse resp2 = aiCacheManager.getOrCompute(cacheKey, "v1", false, supplier);
    assertEquals("diagnosis content", resp2.content());
    assertEquals(1, providerExecutions.get()); // Provider was NOT executed again!

    // Verify token saved metric recorded
    double tokensSaved = meterRegistry.get("ai_token_saved_total").counter().count();
    assertEquals(150.0, tokensSaved);
  }

  @Test
  void testCacheBypass() {
    String cacheKey = "vetra:ai:cache:testkey2";
    AIResponse mockResponse =
        new AIResponse("bypass content", "v1", "noop", "noop-v1", 10, 10, "stop");

    AtomicInteger providerExecutions = new AtomicInteger(0);
    Supplier<AIResponse> supplier =
        () -> {
          providerExecutions.incrementAndGet();
          return mockResponse;
        };

    // Warm cache
    aiCacheManager.getOrCompute(cacheKey, "v1", false, supplier);
    assertEquals(1, providerExecutions.get());

    // Call with bypass = true
    AIResponse respBypass = aiCacheManager.getOrCompute(cacheKey, "v1", true, supplier);
    assertEquals("bypass content", respBypass.content());
    assertEquals(2, providerExecutions.get()); // Execution forced!
  }

  @Test
  void testCacheStampedeProtection_singleFlightExecution() throws Exception {
    String cacheKey = "vetra:ai:cache:stampede_key";
    AIResponse mockResponse =
        new AIResponse("stampede response", "v1", "noop", "noop-v1", 20, 20, "stop");

    AtomicInteger providerExecutions = new AtomicInteger(0);
    CountDownLatch latch = new CountDownLatch(1);

    Supplier<AIResponse> supplier =
        () -> {
          providerExecutions.incrementAndGet();
          try {
            latch.await(); // Simulate slow provider processing
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          return mockResponse;
        };

    ExecutorService executor = Executors.newFixedThreadPool(5);
    try {
      // Launch 5 concurrent requests for the same cache key
      Future<AIResponse> f1 = executor.submit(() -> aiCacheManager.getOrCompute(cacheKey, "v1", false, supplier));
      Future<AIResponse> f2 = executor.submit(() -> aiCacheManager.getOrCompute(cacheKey, "v1", false, supplier));
      Future<AIResponse> f3 = executor.submit(() -> aiCacheManager.getOrCompute(cacheKey, "v1", false, supplier));

      // Release provider execution latch
      Thread.sleep(50);
      latch.countDown();

      AIResponse r1 = f1.get();
      AIResponse r2 = f2.get();
      AIResponse r3 = f3.get();

      assertNotNull(r1);
      assertEquals("stampede response", r1.content());
      assertEquals("stampede response", r2.content());
      assertEquals("stampede response", r3.content());

      // SINGLE-FLIGHT VERIFICATION: Provider executed EXACTLY ONCE across 3 concurrent calls!
      assertEquals(1, providerExecutions.get());

    } finally {
      executor.shutdown();
    }
  }

  @Test
  void testRedisExceptionGracefulFallback() {
    CacheManager mockCacheManager = mock(CacheManager.class);
    Cache mockCache = mock(Cache.class);
    when(mockCacheManager.getCache(CacheNames.AI_DIAGNOSIS)).thenReturn(mockCache);
    when(mockCache.get(anyString())).thenThrow(new RuntimeException("Redis connection refused"));

    AICacheManager resilientManager =
        new AICacheManager(mockCacheManager, properties, meterRegistry, null);

    AIResponse mockResponse =
        new AIResponse("fallback content", "v1", "noop", "noop-v1", 5, 5, "stop");

    // Execution should NOT fail when Redis throws an exception
    AIResponse response =
        resilientManager.getOrCompute("key", "v1", false, () -> mockResponse);

    assertEquals("fallback content", response.content());
  }

  @Test
  void testWarmupCache() {
    String cacheKey = "vetra:ai:cache:warmup_key";
    AIResponse mockResponse =
        new AIResponse("warmup content", "v1", "noop", "noop-v1", 10, 10, "stop");

    aiCacheManager.warmupCache(cacheKey, mockResponse, "v1", null);

    Optional<CachedAIResponse> cached = aiCacheManager.getFromCache(cacheKey);
    assertTrue(cached.isPresent());
    assertEquals("warmup content", cached.get().response().content());
  }
}
