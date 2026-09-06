package app.vetra.vaccination.repository;

import app.vetra.vaccination.entity.VaccinationCampaign;
import app.vetra.vaccination.enums.CampaignStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link VaccinationCampaign}.
 */
@Repository
public interface VaccinationCampaignRepository extends JpaRepository<VaccinationCampaign, UUID> {

  Page<VaccinationCampaign> findByStatus(CampaignStatus status, Pageable pageable);

  Page<VaccinationCampaign> findByTargetDistrictIgnoreCase(String district, Pageable pageable);

  Page<VaccinationCampaign> findByStatusAndTargetDistrictIgnoreCase(
      CampaignStatus status, String district, Pageable pageable);

  List<VaccinationCampaign> findByStatus(CampaignStatus status);

  long countByStatus(CampaignStatus status);

  @Query("SELECT COALESCE(SUM(c.plannedDoses), 0) FROM VaccinationCampaign c")
  long sumPlannedDoses();

  @Query("SELECT COALESCE(SUM(c.administeredDoses), 0) FROM VaccinationCampaign c")
  long sumAdministeredDoses();
}
