package app.vetra.infrastructure.idempotency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdempotencyCleanupServiceTest {

  @Mock private IdempotencyKeyRepository repository;

  @InjectMocks private IdempotencyCleanupService cleanupService;

  @Test
  @DisplayName("Should invoke repository deleteExpiredKeys with current timestamp")
  void shouldPurgeExpiredKeys() {
    when(repository.deleteExpiredKeys(any(Instant.class))).thenReturn(42);

    int count = cleanupService.cleanupExpiredKeys();

    assertEquals(42, count);
    verify(repository, times(1)).deleteExpiredKeys(any(Instant.class));
  }
}
