package app.vetra.infrastructure.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.vetra.infrastructure.redis.properties.RedisProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Unit & Integration test for Redis Infrastructure Foundation (Stage 12.3.1). Verifies Redis
 * templates, connection factory initialization, Spring Cache manager, and property binding.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:vetra_redis_infra_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
      "spring.flyway.enabled=false",
      "vetra.jwt.secret=test-jwt-secret-value-minimum-32-characters-long",
      "vetra.jwt.expiration-ms=86400000",
      "vetra.jwt.refresh-expiration-ms=604800000",
      "vetra.cors.allowed-origins=http://localhost:3000",
      "vetra.cors.allowed-methods=GET,POST,PUT,DELETE,PATCH,OPTIONS",
      "vetra.cors.allowed-headers=*",
      "vetra.cors.allow-credentials=true",
      "vetra.cors.max-age=3600",
      "vetra.aws.region=ap-south-1",
      "vetra.aws.credentials.access-key=test-key",
      "vetra.aws.credentials.secret-key=test-secret",
      "vetra.aws.s3.bucket-name=vetra-test-bucket",
      "vetra.aws.s3.presigned-url-expiry-minutes=15",
      "vetra.ai.enabled=false",
      "vetra.ai.default-provider=NONE",
      "vetra.redis.host=localhost",
      "vetra.redis.port=6379",
      "vetra.redis.password=test_password",
      "vetra.redis.database=0",
      "vetra.redis.timeout=2000ms"
    })
class RedisInfrastructureTest {

  @Autowired private RedisConnectionFactory redisConnectionFactory;
  @Autowired private RedisTemplate<String, Object> redisTemplate;
  @Autowired private StringRedisTemplate stringRedisTemplate;
  @Autowired private CacheManager cacheManager;
  @Autowired private RedisProperties redisProperties;

  @Test
  void testRedisInfrastructureBeansInitialized() {
    assertNotNull(redisConnectionFactory, "RedisConnectionFactory must be initialized");
    assertNotNull(redisTemplate, "RedisTemplate must be initialized");
    assertNotNull(stringRedisTemplate, "StringRedisTemplate must be initialized");
    assertNotNull(cacheManager, "Spring CacheManager must be initialized");
  }

  @Test
  void testRedisPropertiesBinding() {
    assertNotNull(redisProperties);
    assertEquals("localhost", redisProperties.getHost());
    assertEquals(6379, redisProperties.getPort());
    assertEquals("test_password", redisProperties.getPassword());
    assertEquals(0, redisProperties.getDatabase());
  }
}
