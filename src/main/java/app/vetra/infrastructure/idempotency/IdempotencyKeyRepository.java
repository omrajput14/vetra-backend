package app.vetra.infrastructure.idempotency;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Data access repository for IdempotencyKey entity. */
@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {

  Optional<IdempotencyKey> findByUserIdAndIdempotencyKeyAndRequestPath(
      UUID userId, String idempotencyKey, String requestPath);

  @Modifying
  @Query("DELETE FROM IdempotencyKey k WHERE k.expiresAt < :now")
  int deleteExpiredKeys(@Param("now") Instant now);
}
