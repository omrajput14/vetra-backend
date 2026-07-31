package app.vetra.notification.dto;

import app.vetra.notification.entity.Notification;
import app.vetra.notification.entity.NotificationChannel;
import app.vetra.notification.entity.NotificationPriority;
import app.vetra.notification.entity.NotificationStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Public DTO representing a notification payload.
 */
public record NotificationResponse(
    UUID id,
    UUID userId,
    NotificationChannel channel,
    NotificationPriority priority,
    String title,
    String body,
    String payloadJson,
    NotificationStatus status,
    Instant scheduledAt,
    Instant deliveredAt,
    Instant readAt,
    Instant createdAt
) {

  /**
   * Factory method mapping Notification entity to NotificationResponse DTO.
   *
   * @param notification entity instance
   * @return {@link NotificationResponse} DTO
   */
  public static NotificationResponse fromEntity(Notification notification) {
    return new NotificationResponse(
        notification.getId(),
        notification.getUser().getId(),
        notification.getChannel(),
        notification.getPriority(),
        notification.getTitle(),
        notification.getBody(),
        notification.getPayloadJson(),
        notification.getStatus(),
        notification.getScheduledAt(),
        notification.getDeliveredAt(),
        notification.getReadAt(),
        notification.getCreatedAt()
    );
  }
}
