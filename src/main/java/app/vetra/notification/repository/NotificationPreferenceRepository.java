package app.vetra.notification.repository;

import app.vetra.notification.entity.NotificationPreference;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for NotificationPreference entities. */
@Repository
public interface NotificationPreferenceRepository
    extends JpaRepository<NotificationPreference, UUID> {

  /** Finds user notification preference settings by user ID. */
  Optional<NotificationPreference> findByUserId(UUID userId);
}
