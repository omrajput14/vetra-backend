package app.vetra.vaccination.repository;

import app.vetra.vaccination.entity.VaccinationCampaignAuditLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link VaccinationCampaignAuditLog}.
 */
@Repository
public interface VaccinationCampaignAuditLogRepository
    extends JpaRepository<VaccinationCampaignAuditLog, UUID> {

  List<VaccinationCampaignAuditLog> findByCampaignIdOrderByCreatedAtDesc(UUID campaignId);
}
