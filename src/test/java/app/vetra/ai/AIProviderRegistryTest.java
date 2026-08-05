package app.vetra.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.ai.entity.AIProviderType;
import app.vetra.ai.orchestrator.AIProviderRegistry;
import app.vetra.ai.provider.AIProvider;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/** Tests for AIProviderRegistry auto-discovery, type lookup, and health status reporting. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:vetra_registry_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
      "vetra.ai.default-provider=NONE"
    })
class AIProviderRegistryTest {

  @Autowired private AIProviderRegistry registry;

  @Test
  void testProviderAutoDiscoveryAndRegistryLookup() {
    List<AIProviderType> registeredTypes = registry.getRegisteredTypes();
    assertTrue(registeredTypes.contains(AIProviderType.NONE));

    AIProvider defaultProvider = registry.getDefaultProvider();
    assertNotNull(defaultProvider);
    assertEquals("noop", defaultProvider.providerName());
    assertEquals(AIProviderType.NONE, defaultProvider.providerType());
    assertTrue(defaultProvider.isAvailable());

    AIProvider provider = registry.getProvider(AIProviderType.NONE);
    assertEquals("noop", provider.providerName());
  }
}
