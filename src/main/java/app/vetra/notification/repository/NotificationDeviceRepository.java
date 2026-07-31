package app.vetra.notification.repository;

import app.vetra.notification.entity.NotificationDevice;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for NotificationDevice entities.
 */
@Repository
public interface NotificationDeviceRepository extends JpaRepository<NotificationDevice, UUID> {

  /** Finds active registered push device tokens for a user. */
  List<NotificationDevice> findByUserIdAndActiveTrue(UUID userId);

  /** Finds registered device by unique device token string. */
  Optional<NotificationDevice> findByDeviceToken(String deviceToken);
}
