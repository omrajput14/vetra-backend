package app.vetra.ai.repository;

import app.vetra.ai.entity.AIAdvisorSession;
import app.vetra.infrastructure.persistence.entity.Animal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for AI Veterinary Advisor sessions. */
@Repository
public interface AIAdvisorSessionRepository extends JpaRepository<AIAdvisorSession, UUID> {

  /** Finds all sessions for an animal ordered by creation date descending. */
  List<AIAdvisorSession> findByAnimalOrderByCreatedAtDesc(Animal animal);

  /** Finds all sessions for an animal ID with pagination. */
  Page<AIAdvisorSession> findByAnimalIdOrderByCreatedAtDesc(UUID animalId, Pageable pageable);

  /** Finds all sessions initiated by a specific user. */
  Page<AIAdvisorSession> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

  /** Finds a session by ID and animal ID for authorization validation. */
  Optional<AIAdvisorSession> findByIdAndAnimalId(UUID id, UUID animalId);

  /** Finds a session by ID and user ID for authorization validation. */
  Optional<AIAdvisorSession> findByIdAndUserId(UUID id, UUID userId);
}
