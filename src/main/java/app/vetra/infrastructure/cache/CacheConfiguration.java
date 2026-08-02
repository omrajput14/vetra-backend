package app.vetra.infrastructure.cache;

import java.util.HashMap;
import java.util.Map;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Enterprise Spring CacheManager configuration for Vetra platform caching.
 * Configures RedisCacheManager with customized TTL policies per cache region,
 * Jackson JSON serialization, disabling null value caching, and key prefixing.
 */
@Configuration
public class CacheConfiguration {

  /**
   * Provisions the primary {@link CacheManager} bean configured with region-specific TTLs.
   *
   * @param connectionFactory Redis connection factory bean
   * @return configured RedisCacheManager instance
   */
  @Bean
  public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    RedisCacheConfiguration defaultConfig = createBaseCacheConfig()
        .entryTtl(CacheTtl.DEFAULT);

    Map<String, RedisCacheConfiguration> initialConfigs = new HashMap<>();

    initialConfigs.put(CacheNames.OTP,
        createBaseCacheConfig().entryTtl(CacheTtl.OTP));

    initialConfigs.put(CacheNames.DASHBOARD_FARMER,
        createBaseCacheConfig().entryTtl(CacheTtl.DASHBOARD_FARMER));

    initialConfigs.put(CacheNames.DASHBOARD_VET,
        createBaseCacheConfig().entryTtl(CacheTtl.DASHBOARD_VET));

    initialConfigs.put(CacheNames.DASHBOARD_ADMIN,
        createBaseCacheConfig().entryTtl(CacheTtl.DASHBOARD_ADMIN));

    initialConfigs.put(CacheNames.ANIMALS,
        createBaseCacheConfig().entryTtl(CacheTtl.ANIMALS));

    initialConfigs.put(CacheNames.APPOINTMENTS,
        createBaseCacheConfig().entryTtl(CacheTtl.APPOINTMENTS));

    initialConfigs.put(CacheNames.MEDICAL_RECORDS,
        createBaseCacheConfig().entryTtl(CacheTtl.MEDICAL_RECORDS));

    initialConfigs.put(CacheNames.USERS,
        createBaseCacheConfig().entryTtl(CacheTtl.USERS));

    initialConfigs.put(CacheNames.USER_PROFILES,
        createBaseCacheConfig().entryTtl(CacheTtl.USER_PROFILES));

    initialConfigs.put(CacheNames.DISEASE_REPORTS,
        createBaseCacheConfig().entryTtl(CacheTtl.DISEASE_REPORTS));

    initialConfigs.put(CacheNames.OUTBREAKS,
        createBaseCacheConfig().entryTtl(CacheTtl.OUTBREAKS));

    initialConfigs.put(CacheNames.NOTIFICATIONS,
        createBaseCacheConfig().entryTtl(CacheTtl.NOTIFICATIONS));

    initialConfigs.put(CacheNames.SETTINGS,
        createBaseCacheConfig().entryTtl(CacheTtl.SETTINGS));

    initialConfigs.put(CacheNames.REFERENCE_DATA,
        createBaseCacheConfig().entryTtl(CacheTtl.REFERENCE_DATA));

    initialConfigs.put(CacheNames.AI_DIAGNOSIS,
        createBaseCacheConfig().entryTtl(CacheTtl.AI_DIAGNOSIS));

    initialConfigs.put(CacheNames.ANALYTICS,
        createBaseCacheConfig().entryTtl(CacheTtl.ANALYTICS));

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(defaultConfig)
        .withInitialCacheConfigurations(initialConfigs)
        .transactionAware()
        .build();
  }

  /**
   * Builds base {@link RedisCacheConfiguration} with UTF-8 String key serializer,
   * GenericJackson2JsonRedisSerializer value serializer, and null caching disabled.
   *
   * @return base RedisCacheConfiguration template
   */
  private RedisCacheConfiguration createBaseCacheConfig() {
    return RedisCacheConfiguration.defaultCacheConfig()
        .prefixCacheNameWith(CacheKeys.PREFIX)
        .disableCachingNullValues()
        .serializeKeysWith(
            RedisSerializationContext.SerializationPair.fromSerializer(
                new StringRedisSerializer()))
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer()));
  }
}
