package app.vetra.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Refresh token persistence entity storing SHA-256 token hashes. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "refresh_tokens")
public class RefreshToken {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @NotNull
  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @NotNull
  @Column(name = "expiry_date", nullable = false)
  private Instant expiryDate;

  @Builder.Default
  @Column(name = "revoked", nullable = false)
  private boolean revoked = false;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public void setTokenHash(String tokenHash) {
    this.tokenHash = tokenHash;
  }

  public Instant getExpiryDate() {
    return expiryDate;
  }

  public void setExpiryDate(Instant expiryDate) {
    this.expiryDate = expiryDate;
  }

  public boolean isRevoked() {
    return revoked;
  }

  public void setRevoked(boolean revoked) {
    this.revoked = revoked;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public static RefreshTokenBuilder builder() {
    return new RefreshTokenBuilder();
  }

  public static class RefreshTokenBuilder {
    private User user;
    private String tokenHash;
    private Instant expiryDate;
    private boolean revoked = false;
    private Instant createdAt = Instant.now();

    public RefreshTokenBuilder user(User user) {
      this.user = user;
      return this;
    }

    public RefreshTokenBuilder tokenHash(String tokenHash) {
      this.tokenHash = tokenHash;
      return this;
    }

    public RefreshTokenBuilder expiryDate(Instant expiryDate) {
      this.expiryDate = expiryDate;
      return this;
    }

    public RefreshTokenBuilder revoked(boolean revoked) {
      this.revoked = revoked;
      return this;
    }

    public RefreshTokenBuilder createdAt(Instant createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public RefreshToken build() {
      RefreshToken token = new RefreshToken();
      token.setUser(this.user);
      token.setTokenHash(this.tokenHash);
      token.setExpiryDate(this.expiryDate);
      token.setRevoked(this.revoked);
      token.setCreatedAt(this.createdAt);
      return token;
    }
  }
}
