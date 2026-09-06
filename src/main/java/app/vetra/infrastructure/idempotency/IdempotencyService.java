package app.vetra.infrastructure.idempotency;

import app.vetra.auth.repository.UserRepository;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.response.ApiResponse;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service providing scoped idempotency guarantees for mobile offline-first mutations.
 * Key uniqueness is scoped strictly to (user_id, idempotency_key, request_path).
 */
@Service
public class IdempotencyService {

  private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

  private final IdempotencyKeyRepository repository;
  private final UserRepository userRepository;
  private final ObjectMapper objectMapper;

  public IdempotencyService(
      IdempotencyKeyRepository repository,
      UserRepository userRepository,
      ObjectMapper objectMapper) {
    this.repository = repository;
    this.userRepository = userRepository;
    this.objectMapper = objectMapper;
  }

  /**
   * Executes or retrieves the cached result of an idempotent mutation.
   *
   * <ul>
   *   <li>If no key is supplied, executes mutation normally.
   *   <li>If key exists and is unexpired, returns cached response without executing mutation.
   *   <li>If key is new, executes mutation and records response if successful (2xx).
   *   <li>If mutation throws an exception, nothing is cached, allowing safe retries.
   * </ul>
   */
  @Transactional
  public <T> ApiResponse<T> executeOrGet(
      String userIdentifier,
      String idempotencyKey,
      String requestPath,
      Class<T> responseDataType,
      Supplier<ApiResponse<T>> mutationSupplier) {

    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      return mutationSupplier.get();
    }

    final String trimmedKey = idempotencyKey.trim();
    final String trimmedPath = requestPath.trim();

    UUID userId =
        userRepository
            .findByIdentifier(userIdentifier)
            .map(User::getId)
            .orElse(null);

    if (userId == null) {
      return mutationSupplier.get();
    }

    // Check for existing valid cached response
    Optional<IdempotencyKey> existing =
        repository.findByUserIdAndIdempotencyKeyAndRequestPath(
            userId, trimmedKey, trimmedPath);

    if (existing.isPresent() && existing.get().getExpiresAt().isAfter(Instant.now())) {
      log.info(
          "Idempotency cache hit: user={}, key={}, path={}",
          userId,
          trimmedKey,
          trimmedPath);
      return deserializeResponse(existing.get().getResponseBody(), responseDataType);
    }

    // Execute the actual business mutation
    ApiResponse<T> result = mutationSupplier.get();

    // Only persist successful responses (2xx / success = true)
    if (result != null && result.success() && result.status() >= 200 && result.status() < 300) {
      try {
        String json = objectMapper.writeValueAsString(result);
        IdempotencyKey entry =
            new IdempotencyKey(
                userId,
                trimmedKey,
                trimmedPath,
                result.status(),
                json);
        repository.saveAndFlush(entry);
      } catch (DataIntegrityViolationException e) {
        log.warn("Concurrent duplicate idempotency submission detected for user {} key {}", userId, trimmedKey);
        // Another thread committed concurrently — return the winning committed record
        return repository
            .findByUserIdAndIdempotencyKeyAndRequestPath(userId, trimmedKey, trimmedPath)
            .map(cached -> deserializeResponse(cached.getResponseBody(), responseDataType))
            .orElse(result);
      } catch (Exception e) {
        log.warn("Failed to persist idempotency key {}: {}", trimmedKey, e.getMessage());
      }
    }

    return result;
  }

  private <T> ApiResponse<T> deserializeResponse(String json, Class<T> responseDataType) {
    try {
      JavaType javaType =
          objectMapper
              .getTypeFactory()
              .constructParametricType(ApiResponse.class, responseDataType);
      return objectMapper.readValue(json, javaType);
    } catch (Exception e) {
      log.error("Failed to deserialize cached idempotent response: {}", e.getMessage());
      throw new IllegalStateException("Failed to parse cached idempotent response", e);
    }
  }
}
