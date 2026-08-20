package app.vetra.ai.dto.advisor;

import app.vetra.ai.entity.AIAdvisorRiskLevel;
import app.vetra.ai.entity.AIAdvisorSessionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Full response DTO for an AI Veterinary Advisor session. */
public record AIAdvisorSessionResponse(
    UUID id,
    UUID animalId,
    String animalName,
    String species,
    String breed,
    UUID userId,
    AIAdvisorSessionStatus status,
    AIAdvisorRiskLevel riskLevel,
    boolean requiresVetReview,
    int turnCount,
    AIAdvisorAssessmentDTO assessment,
    List<AIAdvisorMessageResponse> messages,
    Instant createdAt,
    Instant updatedAt
) {}
