package app.vetra.auth.repository;

import app.vetra.infrastructure.persistence.entity.RefreshToken;
import app.vetra.infrastructure.persistence.entity.User;
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
}
