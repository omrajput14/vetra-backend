package app.vetra.infrastructure.redis.config;

import app.vetra.infrastructure.redis.properties.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Production-ready Redis infrastructure configuration. Configures Lettuce connection factory, Redis
 * templates with JSON serialization, transaction support, and Spring Cache abstraction enablement.
 */
@Configuration
@EnableCaching
@EnableConfigurationProperties(RedisProperties.class)
public class RedisConfig {

  private final RedisProperties redisProperties;

  /**
   * Constructs {@code RedisConfig} with constructor dependency injection.
   *
   * @param redisProperties strongly typed Redis configuration properties
   */
  public RedisConfig(RedisProperties redisProperties) {
    this.redisProperties = redisProperties;
  }

  /**
   * Creates the {@link RedisConnectionFactory} configured with standalone host, port, database
   * index, optional password, and command execution timeout.
   *
   * @return configured Lettuce RedisConnectionFactory bean
   */
  @Bean
  public RedisConnectionFactory redisConnectionFactory() {
    RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration();
    standaloneConfig.setHostName(redisProperties.getHost());
    standaloneConfig.setPort(redisProperties.getPort());
    standaloneConfig.setDatabase(redisProperties.getDatabase());

    if (redisProperties.getPassword() != null && !redisProperties.getPassword().isBlank()) {
      standaloneConfig.setPassword(RedisPassword.of(redisProperties.getPassword()));
    }

    LettuceClientConfiguration clientConfig =
        LettuceClientConfiguration.builder().commandTimeout(redisProperties.getTimeout()).build();

    return new LettuceConnectionFactory(standaloneConfig, clientConfig);
  }

  /**
   * Creates a {@link RedisTemplate} configured with UTF-8 String key serialization, Jackson JSON
   * value serialization, and transaction support.
   *
   * @param connectionFactory configured RedisConnectionFactory
   * @return typed RedisTemplate bean
   */
  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    StringRedisSerializer stringSerializer = new StringRedisSerializer();
    GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

    template.setKeySerializer(stringSerializer);
    template.setHashKeySerializer(stringSerializer);
    template.setValueSerializer(jsonSerializer);
    template.setHashValueSerializer(jsonSerializer);

    template.setEnableTransactionSupport(true);
    template.afterPropertiesSet();
    return template;
  }

  /**
   * Creates a {@link StringRedisTemplate} for raw UTF-8 string key-value operations.
   *
   * @param connectionFactory configured RedisConnectionFactory
   * @return StringRedisTemplate bean
   */
  @Bean
  public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
    StringRedisTemplate template = new StringRedisTemplate();
    template.setConnectionFactory(connectionFactory);
    template.setEnableTransactionSupport(true);
    template.afterPropertiesSet();
    return template;
  }
}
