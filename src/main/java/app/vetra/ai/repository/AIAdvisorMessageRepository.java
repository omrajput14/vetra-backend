package app.vetra.ai.repository;

import app.vetra.ai.entity.AIAdvisorMessage;
import app.vetra.ai.entity.AIAdvisorSession;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for AI Veterinary Advisor messages. */
@Repository
public interface AIAdvisorMessageRepository extends JpaRepository<AIAdvisorMessage, UUID> {

  /** Finds all messages for a session ordered by creation timestamp ascending. */
  List<AIAdvisorMessage> findBySessionOrderByCreatedAtAsc(AIAdvisorSession session);

  /** Finds all messages for a session ID ordered by turn number ascending. */
  List<AIAdvisorMessage> findBySessionIdOrderByTurnNumberAsc(UUID sessionId);
}
