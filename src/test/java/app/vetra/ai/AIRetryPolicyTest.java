package app.vetra.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.vetra.ai.entity.AIProviderType;
import app.vetra.ai.exception.AIInferenceException;
import app.vetra.ai.orchestrator.AIRetryPolicy;
import app.vetra.ai.provider.AIInferenceResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests for AIRetryPolicy exponential backoff and retry attempt limits.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:vetra_retry_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
    "vetra.ai.retry.max-attempts=2",
    "vetra.ai.retry.backoff=10ms"
})
class AIRetryPolicyTest {

  @Autowired private AIRetryPolicy retryPolicy;

  @Test
  void testRetryPolicySucceedsOnSecondAttempt() {
    AtomicInteger attempts = new AtomicInteger(0);

    AIInferenceResult result = retryPolicy.executeWithRetry(() -> {
      if (attempts.incrementAndGet() == 1) {
        throw new RuntimeException("Transient network issue");
      }
      return new AIInferenceResult(
          AIProviderType.CUSTOM, "custom-v1", "Test Diagnosis",
          new BigDecimal("0.950"), "{}", "REQ-100", 120L, 50, List.of(), Instant.now()
      );
    });

    assertNotNull(result);
    assertEquals("Test Diagnosis", result.diagnosis());
    assertEquals(2, attempts.get());
  }

  @Test
  void testRetryPolicyExhaustsMaxAttempts() {
    AtomicInteger attempts = new AtomicInteger(0);

    assertThrows(RuntimeException.class, () ->
        retryPolicy.executeWithRetry(() -> {
          attempts.incrementAndGet();
          throw new RuntimeException("Persistent service failure");
        }));

    assertEquals(2, attempts.get());
  }
}
