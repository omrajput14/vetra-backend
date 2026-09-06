package app.vetra.infrastructure.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity recording executed client requests by idempotency key.
 * Prevents duplicate processing of mobile offline mutation replays.
 */
@Entity
@Table(
    name = "idempotency_keys",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_idempotency_user_key_path",
          columnNames = {"user_id", "idempotency_key", "request_path"})
    })
public class IdempotencyKey {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "idempotency_key", length = 64, nullable = false)
  private String idempotencyKey;

  @Column(name = "request_path", length = 255, nullable = false)
  private String requestPath;

  @Column(name = "response_status", nullable = false)
  private int responseStatus;

  @Column(name = "response_body", columnDefinition = "TEXT", nullable = false)
  private String responseBody;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt = Instant.now().plusSeconds(172800); // 48 hours

  /** Default constructor for JPA. */
  public IdempotencyKey() {}

  public IdempotencyKey(
      UUID userId, String idempotencyKey, String requestPath, int responseStatus, String responseBody) {
    this.userId = userId;
    this.idempotencyKey = idempotencyKey;
    this.requestPath = requestPath;
    this.responseStatus = responseStatus;
    this.responseBody = responseBody;
    this.createdAt = Instant.now();
    this.expiresAt = Instant.now().plusSeconds(172800);
  }

  // Getters and setters
  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public String getRequestPath() {
    return requestPath;
  }

  public int getResponseStatus() {
    return responseStatus;
  }

  public String getResponseBody() {
    return responseBody;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }
}
