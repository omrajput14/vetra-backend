package app.vetra.animal.repository;

import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.entity.AnimalHealthRecord;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access repository for AnimalHealthRecord entity. */
public interface AnimalHealthRecordRepository extends JpaRepository<AnimalHealthRecord, UUID> {

  /** Finds all health records for an animal ordered chronologically descending (newest first). */
  List<AnimalHealthRecord> findByAnimalIdOrderByRecordedAtDesc(UUID animalId);

  /** Finds all health records for an animal entity ordered chronologically descending. */
  List<AnimalHealthRecord> findByAnimalOrderByRecordedAtDesc(Animal animal);

  /** Finds the most recent health record for an animal. */
  Optional<AnimalHealthRecord> findFirstByAnimalIdOrderByRecordedAtDesc(UUID animalId);
}
