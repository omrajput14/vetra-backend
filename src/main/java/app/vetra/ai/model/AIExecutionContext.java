package app.vetra.ai.model;

import java.util.Map;
import java.util.UUID;

/**
 * Immutable execution context carrying tenant, user, correlation, and metadata information through
 * the AI governance pipeline and gateway.
 *
 * @param tenantId identifier of the tenant executing the request (e.g. "default", "tenant-123")
 * @param userId identifier of the user executing the request
 * @param correlationId unique request tracing ID
 * @param metadata arbitrary contextual metadata
 */
public record AIExecutionContext(
    String tenantId, String userId, String correlationId, Map<String, Object> metadata) {

  /** Compact constructor validating and normalizing null or empty values to defaults. */
  public AIExecutionContext {
    if (tenantId == null || tenantId.isBlank()) {
      tenantId = "default";
    }
    if (userId == null) {
      userId = "system";
    }
    if (correlationId == null || correlationId.isBlank()) {
      correlationId = UUID.randomUUID().toString();
    }
    if (metadata == null) {
      metadata = Map.of();
    } else {
      metadata = Map.copyOf(metadata);
    }
  }

  /**
   * Creates an empty/default context.
   *
   * @return default AIExecutionContext instance
   */
  public static AIExecutionContext empty() {
    return new AIExecutionContext("default", "system", UUID.randomUUID().toString(), Map.of());
  }

  /**
   * Creates a context scoped to a specific tenant.
   *
   * @param tenantId tenant identifier
   * @return tenant-scoped AIExecutionContext instance
   */
  public static AIExecutionContext ofTenant(String tenantId) {
    return new AIExecutionContext(tenantId, "system", UUID.randomUUID().toString(), Map.of());
  }

  /**
   * Creates a context with tenant ID and user ID.
   *
   * @param tenantId tenant identifier
   * @param userId user identifier
   * @return tenant and user scoped AIExecutionContext
   */
  public static AIExecutionContext of(String tenantId, String userId) {
    return new AIExecutionContext(tenantId, userId, UUID.randomUUID().toString(), Map.of());
  }
}
