package app.vetra.auth.repository;

import app.vetra.infrastructure.persistence.entity.FarmerProfile;
import app.vetra.infrastructure.persistence.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access repository for FarmerProfile entity. */
public interface FarmerProfileRepository extends JpaRepository<FarmerProfile, UUID> {

  /** Finds farmer profile by user entity. */
  Optional<FarmerProfile> findByUser(User user);

  /** Finds farmer profile by user id. */
  Optional<FarmerProfile> findByUserId(UUID userId);
}
