package app.vetra.disease.repository;

import app.vetra.disease.entity.Outbreak;
import app.vetra.disease.entity.OutbreakStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for Outbreak cluster entities.
 */
@Repository
public interface OutbreakRepository extends JpaRepository<Outbreak, UUID> {

  /** Finds outbreaks matching status ordered by creation date descending. */
  List<Outbreak> findByStatusOrderByCreatedAtDesc(OutbreakStatus status);

  /** Finds outbreaks by disease name with pagination. */
  Page<Outbreak> findByDiseaseNameIgnoreCase(String diseaseName, Pageable pageable);

  /** Finds active outbreaks for a specific disease. */
  List<Outbreak> findByDiseaseNameIgnoreCaseAndStatus(String diseaseName, OutbreakStatus status);
}
