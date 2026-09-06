package app.vetra.infrastructure.idempotency;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled background service that automatically purges expired idempotency keys
 * to keep the database lean and performant.
 */
@Service
public class IdempotencyCleanupService {

  private static final Logger log = LoggerFactory.getLogger(IdempotencyCleanupService.class);

  private final IdempotencyKeyRepository repository;

  public IdempotencyCleanupService(IdempotencyKeyRepository repository) {
    this.repository = repository;
  }

  /**
   * Cleans up expired idempotency keys daily at 03:00 UTC (configurable via application.yml).
   */
  @Scheduled(cron = "${vetra.idempotency.cleanup.cron:0 0 3 * * *}")
  @Transactional
  public int cleanupExpiredKeys() {
    Instant now = Instant.now();
    int deleted = repository.deleteExpiredKeys(now);
    if (deleted > 0) {
      log.info("Automatic idempotency cleanup purged {} expired key(s)", deleted);
    }
    return deleted;
  }
}
