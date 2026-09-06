package app.vetra.mortality.repository;

import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.entity.FarmerProfile;
import app.vetra.mortality.entity.AnimalMortalityEvent;
import app.vetra.mortality.enums.MortalityReportStatus;
import app.vetra.mortality.enums.MortalitySource;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository interface for animal mortality events. */
@Repository
public interface AnimalMortalityEventRepository extends JpaRepository<AnimalMortalityEvent, UUID> {

  boolean existsByAnimal(Animal animal);

  boolean existsByAnimalId(UUID animalId);

  Optional<AnimalMortalityEvent> findByAnimal(Animal animal);

  Optional<AnimalMortalityEvent> findByAnimalId(UUID animalId);

  Page<AnimalMortalityEvent> findByFarmer(FarmerProfile farmer, Pageable pageable);

  List<AnimalMortalityEvent> findByFarmer(FarmerProfile farmer);

  /** Counts total mortality events by reporting/verification source. */
  long countBySource(MortalitySource source);

  /** Counts total mortality events by report review status. */
  long countByStatus(MortalityReportStatus status);

  /** Counts mortality events matching status and source. */
  long countByStatusAndSource(MortalityReportStatus status, MortalitySource source);

  /**
   * Finds mortality events matching disease name (either farmer reported or vet confirmed)
   * reported within a sliding temporal window.
   */
  @Query("""
      SELECT m FROM AnimalMortalityEvent m
      WHERE (LOWER(m.diseaseName) = LOWER(:diseaseName) OR LOWER(m.vetDiseaseName) = LOWER(:diseaseName))
        AND m.reportedAt >= :cutoffTime
      ORDER BY m.reportedAt DESC
      """)
  List<AnimalMortalityEvent> findByDiseaseNameAndReportedAtAfter(
      @Param("diseaseName") String diseaseName,
      @Param("cutoffTime") Instant cutoffTime);

  /** Finds distinct animal IDs with confirmed mortality events. */
  @Query("SELECT DISTINCT m.animal.id FROM AnimalMortalityEvent m WHERE m.status = app.vetra.mortality.enums.MortalityReportStatus.CONFIRMED")
  List<UUID> findConfirmedMortalityAnimalIds();
}


