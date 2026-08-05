package app.vetra.auth.service;

import app.vetra.auth.repository.RefreshTokenRepository;
import app.vetra.infrastructure.config.JwtProperties;
import app.vetra.infrastructure.persistence.entity.RefreshToken;
import app.vetra.infrastructure.persistence.entity.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing database-backed refresh tokens using SHA-256 token hashing and SecureRandom byte
 * generation.
 */
@Service
public class RefreshTokenService {

  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtProperties jwtProperties;
  private final SecureRandom secureRandom;

  /** Constructor injection. */
  public RefreshTokenService(
      RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties) {
    this.refreshTokenRepository = refreshTokenRepository;
    this.jwtProperties = jwtProperties;
    this.secureRandom = new SecureRandom();
  }

  /**
   * Generates a 32-byte SecureRandom URL-safe Base64 raw token, computes its SHA-256 hash, stores
   * ONLY the hash, and returns the raw token string.
   */
  @Transactional
  public String createRefreshToken(User user) {
    refreshTokenRepository.deleteByUser(user);

    byte[] randomBytes = new byte[32];
    secureRandom.nextBytes(randomBytes);
    String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    String tokenHash = hashToken(rawToken);

    RefreshToken refreshToken =
        RefreshToken.builder()
            .user(user)
            .tokenHash(tokenHash)
            .expiryDate(Instant.now().plusMillis(jwtProperties.refreshExpirationMs()))
            .revoked(false)
            .build();

    refreshTokenRepository.save(refreshToken);
    return rawToken;
  }

  /** Finds refresh token entity by hashing incoming raw token string. */
  @Transactional(readOnly = true)
  public Optional<RefreshToken> findByRawToken(String rawToken) {
    String tokenHash = hashToken(rawToken);
    return refreshTokenRepository.findByTokenHash(tokenHash);
  }

  /** Verifies token expiration and revocation. */
  @Transactional
  public RefreshToken verifyExpiration(RefreshToken token) {
    if (token.getExpiryDate().isBefore(Instant.now()) || token.isRevoked()) {
      refreshTokenRepository.delete(token);
      throw new IllegalArgumentException("Refresh token has expired or been revoked");
    }
    return token;
  }

  /** Revokes token session by hashing raw token string. */
  @Transactional
  public void revokeToken(String rawToken) {
    String tokenHash = hashToken(rawToken);
    refreshTokenRepository
        .findByTokenHash(tokenHash)
        .ifPresent(
            t -> {
              t.setRevoked(true);
              refreshTokenRepository.save(t);
            });
  }

  /** Deletes all refresh token sessions belonging to a user (used on password change). */
  @Transactional
  public void revokeAllUserTokens(User user) {
    refreshTokenRepository.deleteByUser(user);
  }

  /** Hashes raw token string with SHA-256. */
  public String hashToken(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashBytes);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 algorithm not available", ex);
    }
  }
}
