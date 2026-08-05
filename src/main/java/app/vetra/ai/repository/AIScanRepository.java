package app.vetra.ai.repository;

import app.vetra.ai.entity.AIScan;
import app.vetra.ai.entity.AIScanStatus;
import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.entity.User;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for AIScan entities. */
@Repository
public interface AIScanRepository extends JpaRepository<AIScan, UUID> {

  /** Finds scans for an animal ordered by creation date descending. */
  List<AIScan> findByAnimalOrderByCreatedAtDesc(Animal animal);

  /** Finds scans for an animal by animal ID with pagination. */
  Page<AIScan> findByAnimalId(UUID animalId, Pageable pageable);

  /** Finds scans by status ordered by creation date descending. */
  List<AIScan> findByStatusOrderByCreatedAtDesc(AIScanStatus status);

  /** Finds scans by status with pagination. */
  Page<AIScan> findByStatus(AIScanStatus status, Pageable pageable);

  /** Finds scans uploaded by a user ordered by creation date descending. */
  List<AIScan> findByUploadedByOrderByCreatedAtDesc(User uploadedBy);

  /** Finds scans uploaded by user ID with pagination. */
  Page<AIScan> findByUploadedById(UUID uploadedById, Pageable pageable);
}
