package app.vetra.auth.repository;

import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.infrastructure.persistence.enums.VerificationStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

  /**
   * Finds all active veterinarians whose verification status is not rejected.
   *
   * @param rejectedStatus status to exclude (typically REJECTED)
   * @return list of active non-rejected veterinarian profiles
   */
  @Query("SELECT v FROM VetProfile v JOIN v.user u WHERE u.isActive = true AND v.verificationStatus != :rejectedStatus")
  List<VetProfile> findAllActiveNonRejected(@Param("rejectedStatus") VerificationStatus rejectedStatus);
}
