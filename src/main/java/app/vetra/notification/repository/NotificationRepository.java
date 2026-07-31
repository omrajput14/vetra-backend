package app.vetra.notification.repository;

import app.vetra.notification.entity.Notification;
import app.vetra.notification.entity.NotificationStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for Notification entities.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  /** Finds notifications for a user ordered by creation date descending. */
  Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

  /** Counts unread notifications for a user. */
  long countByUserIdAndReadAtIsNull(UUID userId);

  /** Finds pending or queued notifications due for delivery retry. */
  List<Notification> findByStatusAndScheduledAtBefore(NotificationStatus status, Instant before);
}
