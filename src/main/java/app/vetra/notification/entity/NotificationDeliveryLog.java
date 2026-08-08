package app.vetra.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Audit log recording individual provider transmission attempts and response payloads. */
@Entity
@Table(name = "notification_delivery_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDeliveryLog {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "notification_id", nullable = false)
  private Notification notification;

  @Column(name = "provider", nullable = false, length = 64)
  private String provider;

  @Column(name = "status", nullable = false, length = 32)
  private String status;

  @Column(name = "response_payload", columnDefinition = "TEXT")
  private String responsePayload;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Column(name = "attempt_number", nullable = false)
  @Builder.Default
  private Integer attemptNumber = 1;

  @Column(name = "created_at", nullable = false, updatable = false)
  @Builder.Default
  private Instant createdAt = Instant.now();

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Notification getNotification() {
    return notification;
  }

  public void setNotification(Notification notification) {
    this.notification = notification;
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getResponsePayload() {
    return responsePayload;
  }

  public void setResponsePayload(String responsePayload) {
    this.responsePayload = responsePayload;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public Integer getAttemptNumber() {
    return attemptNumber;
  }

  public void setAttemptNumber(Integer attemptNumber) {
    this.attemptNumber = attemptNumber;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public static NotificationDeliveryLogBuilder builder() {
    return new NotificationDeliveryLogBuilder();
  }

  public static class NotificationDeliveryLogBuilder {
    private Notification notification;
    private String provider;
    private String status;
    private String responsePayload;
    private String errorMessage;
    private Integer attemptNumber = 1;
    private Instant createdAt = Instant.now();

    public NotificationDeliveryLogBuilder notification(Notification notification) {
      this.notification = notification;
      return this;
    }

    public NotificationDeliveryLogBuilder provider(String provider) {
      this.provider = provider;
      return this;
    }

    public NotificationDeliveryLogBuilder status(String status) {
      this.status = status;
      return this;
    }

    public NotificationDeliveryLogBuilder responsePayload(String responsePayload) {
      this.responsePayload = responsePayload;
      return this;
    }

    public NotificationDeliveryLogBuilder errorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
      return this;
    }

    public NotificationDeliveryLogBuilder attemptNumber(Integer attemptNumber) {
      this.attemptNumber = attemptNumber;
      return this;
    }

    public NotificationDeliveryLogBuilder createdAt(Instant createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public NotificationDeliveryLog build() {
      NotificationDeliveryLog log = new NotificationDeliveryLog();
      log.setNotification(this.notification);
      log.setProvider(this.provider);
      log.setStatus(this.status);
      log.setResponsePayload(this.responsePayload);
      log.setErrorMessage(this.errorMessage);
      log.setAttemptNumber(this.attemptNumber);
      log.setCreatedAt(this.createdAt);
      return log;
    }
  }
}
