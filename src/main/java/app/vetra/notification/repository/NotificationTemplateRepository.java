package app.vetra.notification.repository;

import app.vetra.notification.entity.NotificationTemplate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for NotificationTemplate entities. */
@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

  /** Finds notification template by unique code. */
  Optional<NotificationTemplate> findByCode(String code);
}
