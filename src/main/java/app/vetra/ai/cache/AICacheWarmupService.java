package app.vetra.ai.cache;

import app.vetra.ai.model.AIResponse;
import java.time.Duration;

/**
 * Strategy interface and extension point for future automated AI cache warmup and preloading.
 */
public interface AICacheWarmupService {

  /**
   * Preloads a response into the AI cache.
   *
   * @param cacheKey deterministic key
   * @param response response to cache
   * @param promptVersion prompt version
   * @param ttl custom time to live
   */
  void warmupCache(String cacheKey, AIResponse response, String promptVersion, Duration ttl);
}
