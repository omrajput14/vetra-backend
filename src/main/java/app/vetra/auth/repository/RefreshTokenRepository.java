package app.vetra.auth.repository;

import app.vetra.infrastructure.persistence.entity.RefreshToken;
import app.vetra.infrastructure.persistence.entity.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

/** Data access repository for RefreshToken entity. */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

  /** Finds refresh token entity by SHA-256 token hash. */
  Optional<RefreshToken> findByTokenHash(String tokenHash);

  /** Deletes all refresh tokens belonging to a user. */
  @Modifying
  void deleteByUser(User user);

  /** Counts active (unrevoked and unexpired) refresh token sessions. */
  long countByRevokedFalseAndExpiryDateAfter(Instant now);

  /** Counts revoked refresh tokens. */
  long countByRevokedTrue();

  /** Finds the most recent refresh token for a user. */
  Optional<RefreshToken> findFirstByUserOrderByCreatedAtDesc(User user);

  /** Finds top recent sessions ordered by creation date. */
  List<RefreshToken> findTop50ByOrderByCreatedAtDesc();
}
