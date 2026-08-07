package app.vetra.ai.cache;

import app.vetra.ai.model.AIResponse;
import java.time.Instant;

/**
 * Immutable cached representation of an {@link AIResponse}. Stores normalized inference output,
 * cached timestamp, prompt version, and cache key metadata. Sensitive headers, API keys, or
 * governance metadata are strictly omitted.
 *
 * @param response standard normalized AI response
 * @param cachedAt timestamp when response was cached
 * @param promptVersion prompt template version
 * @param cacheKey deterministic cache key
 */
public record CachedAIResponse(
    AIResponse response,
    Instant cachedAt,
    String promptVersion,
    String cacheKey) {

  /**
   * Factory method to wrap an AIResponse into CachedAIResponse.
   *
   * @param response the AIResponse to wrap
   * @param promptVersion prompt version
   * @param cacheKey cache key
   * @return new CachedAIResponse instance
   */
  public static CachedAIResponse of(AIResponse response, String promptVersion, String cacheKey) {
    return new CachedAIResponse(response, Instant.now(), promptVersion, cacheKey);
  }
}
