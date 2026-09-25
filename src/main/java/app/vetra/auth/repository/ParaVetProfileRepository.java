package app.vetra.auth.repository;

import app.vetra.infrastructure.persistence.entity.ParaVetProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Para-vet profiles. */
public interface ParaVetProfileRepository extends JpaRepository<ParaVetProfile, UUID> {

  Optional<ParaVetProfile> findByUserId(UUID userId);
}
