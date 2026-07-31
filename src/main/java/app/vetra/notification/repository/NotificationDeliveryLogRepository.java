package app.vetra.notification.repository;

import app.vetra.notification.entity.NotificationDeliveryLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for NotificationDeliveryLog audit entries.
 */
@Repository
public interface NotificationDeliveryLogRepository extends JpaRepository<NotificationDeliveryLog, UUID> {

  /** Finds delivery log entries for a notification. */
  List<NotificationDeliveryLog> findByNotificationId(UUID notificationId);
}
