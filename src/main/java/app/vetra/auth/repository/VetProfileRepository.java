package app.vetra.auth.repository;

import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.entity.VetProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access repository for VetProfile entity. */
public interface VetProfileRepository extends JpaRepository<VetProfile, UUID> {

  /** Finds vet profile by user entity. */
  Optional<VetProfile> findByUser(User user);

  /** Finds vet profile by user id. */
  Optional<VetProfile> findByUserId(UUID userId);

  /** Checks if registration number exists. */
  boolean existsByRegistrationNumber(String registrationNumber);
}
