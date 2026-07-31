package app.vetra.notification.entity;

import app.vetra.infrastructure.persistence.entity.BaseEntity;
import app.vetra.infrastructure.persistence.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registered mobile or web client device token for push notification dispatch.
 */
@Entity
@Table(name = "notification_devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDevice extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "device_token", nullable = false, unique = true, length = 512)
  private String deviceToken;

  @Column(name = "platform", nullable = false, length = 32)
  @Builder.Default
  private String platform = "ANDROID";

  @Column(name = "app_version", length = 32)
  private String appVersion;

  @Column(name = "active", nullable = false)
  @Builder.Default
  private boolean active = true;

  @Column(name = "last_seen", nullable = false)
  @Builder.Default
  private Instant lastSeen = Instant.now();
}
