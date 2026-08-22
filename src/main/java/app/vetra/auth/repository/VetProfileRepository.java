package app.vetra.auth.repository;

import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.infrastructure.persistence.enums.VerificationStatus;
import java.util.List;
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

  /** Finds all vet profiles matching a specific verification status. */
  List<VetProfile> findByVerificationStatus(VerificationStatus verificationStatus);
}
