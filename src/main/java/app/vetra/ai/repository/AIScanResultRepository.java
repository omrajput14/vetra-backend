package app.vetra.ai.repository;

import app.vetra.ai.entity.AIScan;
import app.vetra.ai.entity.AIScanResultEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for AIScanResultEntity inference records.
 */
@Repository
public interface AIScanResultRepository extends JpaRepository<AIScanResultEntity, UUID> {

  /** Finds all inference result iterations for a given AIScan ordered by created date descending. */
  List<AIScanResultEntity> findByScanOrderByCreatedAtDesc(AIScan scan);

  /** Finds all inference results for a given scan ID. */
  List<AIScanResultEntity> findByScanIdOrderByCreatedAtDesc(UUID scanId);
}
