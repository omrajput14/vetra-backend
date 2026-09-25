package app.vetra.disease.repository;

import app.vetra.disease.entity.AlertAction;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Officer actions on operational alerts. */
public interface AlertActionRepository extends JpaRepository<AlertAction, UUID> {

  Optional<AlertAction> findByAlertId(UUID alertId);
}
