package app.vetra.notification.entity;

import app.vetra.infrastructure.persistence.entity.BaseEntity;
import app.vetra.infrastructure.persistence.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Entity representing an individual notification instance dispatched to a user.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "template_id")
  private NotificationTemplate template;

  @Enumerated(EnumType.STRING)
  @Column(name = "channel", nullable = false, length = 32)
  @Builder.Default
  private NotificationChannel channel = NotificationChannel.PUSH;

  @Enumerated(EnumType.STRING)
  @Column(name = "priority", nullable = false, length = 32)
  @Builder.Default
  private NotificationPriority priority = NotificationPriority.NORMAL;

  @Column(name = "title", nullable = false, length = 256)
  private String title;

  @Column(name = "body", nullable = false, columnDefinition = "TEXT")
  private String body;

  @Column(name = "payload_json", columnDefinition = "TEXT")
  private String payloadJson;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  @Builder.Default
  private NotificationStatus status = NotificationStatus.PENDING;

  @Column(name = "scheduled_at")
  @Builder.Default
  private Instant scheduledAt = Instant.now();

  @Column(name = "delivered_at")
  private Instant deliveredAt;

  @Column(name = "read_at")
  private Instant readAt;
}
