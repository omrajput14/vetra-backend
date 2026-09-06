package app.vetra.infrastructure.idempotency;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import app.vetra.auth.repository.UserRepository;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

  @Mock private IdempotencyKeyRepository repository;
  @Mock private UserRepository userRepository;

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  private IdempotencyService idempotencyService;

  private final UUID userId = UUID.randomUUID();
  private final String userEmail = "farmer@vetra.app";

  @BeforeEach
  void setUp() {
    idempotencyService = new IdempotencyService(repository, userRepository, objectMapper);
  }

  @Test
  @DisplayName("Should execute mutation when no idempotency key is supplied")
  void shouldExecuteMutationWithoutIdempotencyKey() {
    AtomicInteger callCount = new AtomicInteger(0);

    ApiResponse<String> response =
        idempotencyService.executeOrGet(
            userEmail,
            null,
            "/api/v1/animals",
            String.class,
            () -> {
              callCount.incrementAndGet();
              return ApiResponse.ok("Created", "test-data");
            });

    assertEquals(1, callCount.get());
    assertEquals("test-data", response.data());
    verifyNoInteractions(repository);
  }

  @Test
  @DisplayName("Should execute mutation and store response when key is new")
  void shouldExecuteAndStoreWhenKeyIsNew() {
    User mockUser = new User();
    mockUser.setId(userId);
    when(userRepository.findByIdentifier(userEmail)).thenReturn(Optional.of(mockUser));
    when(repository.findByUserIdAndIdempotencyKeyAndRequestPath(any(), any(), any()))
        .thenReturn(Optional.empty());

    AtomicInteger callCount = new AtomicInteger(0);
    String key = "test-uuid-123";

    ApiResponse<String> response =
        idempotencyService.executeOrGet(
            userEmail,
            key,
            "/api/v1/animals",
            String.class,
            () -> {
              callCount.incrementAndGet();
              return ApiResponse.ok("Created", "animal-uuid-1");
            });

    assertEquals(1, callCount.get());
    assertEquals("animal-uuid-1", response.data());
    verify(repository, times(1)).saveAndFlush(any(IdempotencyKey.class));
  }

  @Test
  @DisplayName("Should return cached response and not re-execute mutation when key is repeated")
  void shouldReturnCachedResponseOnDuplicateKey() throws Exception {
    User mockUser = new User();
    mockUser.setId(userId);
    when(userRepository.findByIdentifier(userEmail)).thenReturn(Optional.of(mockUser));

    String key = "test-uuid-123";
    String path = "/api/v1/animals";
    ApiResponse<String> originalResponse = ApiResponse.created("Success", "animal-uuid-1");
    String json = objectMapper.writeValueAsString(originalResponse);

    IdempotencyKey cachedEntry =
        new IdempotencyKey(userId, key, path, 201, json);

    when(repository.findByUserIdAndIdempotencyKeyAndRequestPath(userId, key, path))
        .thenReturn(Optional.of(cachedEntry));

    AtomicInteger callCount = new AtomicInteger(0);

    ApiResponse<String> response =
        idempotencyService.executeOrGet(
            userEmail,
            key,
            path,
            String.class,
            () -> {
              callCount.incrementAndGet();
              return ApiResponse.ok("Should not run", "different-data");
            });

    assertEquals(0, callCount.get(), "Mutation supplier must NOT be invoked on cache hit");
    assertEquals("animal-uuid-1", response.data());
    verify(repository, never()).saveAndFlush(any());
  }

  @Test
  @DisplayName("Should handle concurrent database insert race condition gracefully")
  void shouldHandleConcurrentRaceConditionGracefully() throws Exception {
    User mockUser = new User();
    mockUser.setId(userId);
    when(userRepository.findByIdentifier(userEmail)).thenReturn(Optional.of(mockUser));

    String key = "concurrent-key";
    String path = "/api/v1/animals";
    ApiResponse<String> originalResponse = ApiResponse.created("Success", "animal-concurrent-1");
    String json = objectMapper.writeValueAsString(originalResponse);
    IdempotencyKey winnerEntry = new IdempotencyKey(userId, key, path, 201, json);

    // Initial check returns empty (simulating race before insert)
    when(repository.findByUserIdAndIdempotencyKeyAndRequestPath(userId, key, path))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(winnerEntry));

    // Save throws unique constraint violation because competitor thread committed first
    doThrow(new DataIntegrityViolationException("Unique constraint violation"))
        .when(repository)
        .saveAndFlush(any(IdempotencyKey.class));

    ApiResponse<String> response =
        idempotencyService.executeOrGet(
            userEmail,
            key,
            path,
            String.class,
            () -> ApiResponse.created("Created", "animal-concurrent-1"));

    assertNotNull(response);
    assertEquals("animal-concurrent-1", response.data());
  }

  @Test
  @DisplayName("Should not cache failed non-2xx error responses")
  void shouldNotCacheErrorResponses() {
    User mockUser = new User();
    mockUser.setId(userId);
    when(userRepository.findByIdentifier(userEmail)).thenReturn(Optional.of(mockUser));
    when(repository.findByUserIdAndIdempotencyKeyAndRequestPath(any(), any(), any()))
        .thenReturn(Optional.empty());

    ApiResponse<String> response =
        idempotencyService.executeOrGet(
            userEmail,
            "error-key",
            "/api/v1/animals",
            String.class,
            () -> ApiResponse.error(HttpStatus.BAD_REQUEST, "Invalid animal tag"));

    assertFalse(response.success());
    assertEquals(400, response.status());
    verify(repository, never()).saveAndFlush(any());
  }
}
