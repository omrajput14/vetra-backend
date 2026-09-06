package app.vetra.mortality.repository;

import app.vetra.mortality.entity.MortalityReviewAudit;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository managing persistent audit records of mortality review and validation actions. */
@Repository
public interface MortalityReviewAuditRepository extends JpaRepository<MortalityReviewAudit, UUID> {

  /** Retrieves all audit entries for a mortality event ordered by creation time ascending. */
  List<MortalityReviewAudit> findByMortalityEventIdOrderByCreatedAtAsc(UUID mortalityEventId);
}
