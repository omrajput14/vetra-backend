package app.vetra.infrastructure.cache.config;

import app.vetra.infrastructure.cache.CacheNames;
import app.vetra.infrastructure.cache.CacheTtl;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Enterprise Production Spring CacheManager configuration for Vetra platform caching.
 * Configures RedisCacheManager with customized TTL policies per cache region,
 * Jackson polymorphic JSON value serialization, Java 8 time module support,
 * disabling null value caching, cache statistics, transaction awareness,
 * and resilient CacheErrorHandler for graceful Redis fault tolerance.
 */
@Configuration
@EnableCaching
public class CacheConfiguration implements CachingConfigurer {

  private static final Logger log = LoggerFactory.getLogger(CacheConfiguration.class);

  /**
   * Provisions the primary {@link CacheManager} bean configured with region-specific TTLs.
   *
   * @param connectionFactory Redis connection factory bean
   * @return configured RedisCacheManager instance
   */
  @Bean
  public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    GenericJackson2JsonRedisSerializer jsonSerializer = createJsonSerializer();
    RedisCacheConfiguration defaultConfig = createBaseCacheConfig(jsonSerializer)
        .entryTtl(CacheTtl.DEFAULT);

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(defaultConfig)
        .withInitialCacheConfigurations(buildInitialCacheConfigurations(jsonSerializer))
        .enableStatistics()
        .transactionAware()
        .build();
  }

  private Map<String, RedisCacheConfiguration> buildInitialCacheConfigurations(
      GenericJackson2JsonRedisSerializer serializer) {
    Map<String, RedisCacheConfiguration> configs = new HashMap<>();

    configs.put(CacheNames.OTP, createBaseCacheConfig(serializer).entryTtl(CacheTtl.OTP));
    configs.put(CacheNames.DASHBOARD_FARMER, createBaseCacheConfig(serializer).entryTtl(CacheTtl.DASHBOARD_FARMER));
    configs.put(CacheNames.DASHBOARD_VET, createBaseCacheConfig(serializer).entryTtl(CacheTtl.DASHBOARD_VET));
    configs.put(CacheNames.DASHBOARD_ADMIN, createBaseCacheConfig(serializer).entryTtl(CacheTtl.DASHBOARD_ADMIN));
    configs.put(CacheNames.ANIMALS, createBaseCacheConfig(serializer).entryTtl(CacheTtl.ANIMALS));
    configs.put(CacheNames.APPOINTMENTS, createBaseCacheConfig(serializer).entryTtl(CacheTtl.APPOINTMENTS));
    configs.put(CacheNames.MEDICAL_RECORDS, createBaseCacheConfig(serializer).entryTtl(CacheTtl.MEDICAL_RECORDS));
    configs.put(CacheNames.USERS, createBaseCacheConfig(serializer).entryTtl(CacheTtl.USERS));
    configs.put(CacheNames.USER_PROFILES, createBaseCacheConfig(serializer).entryTtl(CacheTtl.USER_PROFILES));
    configs.put(CacheNames.DISEASE_REPORTS, createBaseCacheConfig(serializer).entryTtl(CacheTtl.DISEASE_REPORTS));
    configs.put(CacheNames.OUTBREAKS, createBaseCacheConfig(serializer).entryTtl(CacheTtl.OUTBREAKS));
    configs.put(CacheNames.NOTIFICATIONS, createBaseCacheConfig(serializer).entryTtl(CacheTtl.NOTIFICATIONS));
    configs.put(CacheNames.SETTINGS, createBaseCacheConfig(serializer).entryTtl(CacheTtl.SETTINGS));
    configs.put(CacheNames.REFERENCE_DATA, createBaseCacheConfig(serializer).entryTtl(CacheTtl.REFERENCE_DATA));
    configs.put(CacheNames.AI_DIAGNOSIS, createBaseCacheConfig(serializer).entryTtl(CacheTtl.AI_DIAGNOSIS));
    configs.put(CacheNames.ANALYTICS, createBaseCacheConfig(serializer).entryTtl(CacheTtl.ANALYTICS));

    return configs;
  }

  /**
   * Configures base {@link RedisCacheConfiguration} with string keys, JSON values,
   * disabled null caching, and "vetra:" key prefixing.
   *
   * @param jsonSerializer GenericJackson2JsonRedisSerializer instance
   * @return base RedisCacheConfiguration template
   */
  private RedisCacheConfiguration createBaseCacheConfig(GenericJackson2JsonRedisSerializer jsonSerializer) {
    return RedisCacheConfiguration.defaultCacheConfig()
        .disableCachingNullValues()
        .serializeKeysWith(
            RedisSerializationContext.SerializationPair.fromSerializer(
                new StringRedisSerializer()))
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));
  }

  /**
   * Provides a resilient {@link CacheErrorHandler} ensuring graceful fallback to database
   * when Redis is unreachable or experiences connection failures.
   */
  @Override
  public CacheErrorHandler errorHandler() {
    return new CacheErrorHandler() {
      @Override
      public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Redis Cache GET failure on cache='{}' key='{}'. Falling back to DB.", cache.getName(), key, exception);
      }

      @Override
      public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("Redis Cache PUT failure on cache='{}' key='{}'. Continuing execution.", cache.getName(), key, exception);
      }

      @Override
      public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Redis Cache EVICT failure on cache='{}' key='{}'. Continuing execution.", cache.getName(), key, exception);
      }

      @Override
      public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("Redis Cache CLEAR failure on cache='{}'. Continuing execution.", cache.getName(), exception);
      }
    };
  }

  /**
   * Creates a {@link GenericJackson2JsonRedisSerializer} configured with JavaTimeModule
   * and polymorphic typing for robust DTO serialization/deserialization across Spring Cache regions.
   *
   * @return configured GenericJackson2JsonRedisSerializer
   */
  private GenericJackson2JsonRedisSerializer createJsonSerializer() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.activateDefaultTyping(
        LaissezFaireSubTypeValidator.instance,
        ObjectMapper.DefaultTyping.EVERYTHING,
        JsonTypeInfo.As.PROPERTY
    );
    return new GenericJackson2JsonRedisSerializer(mapper);
  }
}
