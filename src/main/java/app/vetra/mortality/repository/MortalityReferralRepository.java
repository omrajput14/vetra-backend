package app.vetra.mortality.repository;

import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.mortality.entity.AnimalMortalityEvent;
import app.vetra.mortality.entity.MortalityReferral;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository managing targeted veterinarian referrals for livestock mortality review. */
@Repository
public interface MortalityReferralRepository extends JpaRepository<MortalityReferral, UUID> {

  /** Finds referrals for a veterinarian matching specified statuses. */
  Page<MortalityReferral> findByVeterinarianAndStatusIn(
      VetProfile veterinarian, Collection<String> statuses, Pageable pageable);

  /** Checks if a referral exists linking a mortality event to a veterinarian. */
  boolean existsByMortalityEventAndVeterinarian(
      AnimalMortalityEvent mortalityEvent, VetProfile veterinarian);

  /** Finds a specific referral between a mortality event and a veterinarian. */
  Optional<MortalityReferral> findByMortalityEventAndVeterinarian(
      AnimalMortalityEvent mortalityEvent, VetProfile veterinarian);

  /** Finds all referrals for a mortality event. */
  List<MortalityReferral> findByMortalityEventId(UUID mortalityEventId);

  /**
   * Retrieves pending mortality events assigned to a veterinarian via referrals.
   */
  @Query(
      "SELECT r.mortalityEvent FROM MortalityReferral r "
          + "WHERE r.veterinarian = :vet AND r.status IN :statuses")
  Page<AnimalMortalityEvent> findMortalityEventsByVetAndStatusIn(
      @Param("vet") VetProfile vet,
      @Param("statuses") Collection<String> statuses,
      Pageable pageable);
}
