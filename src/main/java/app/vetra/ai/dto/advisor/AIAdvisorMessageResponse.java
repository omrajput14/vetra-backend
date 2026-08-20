package app.vetra.ai.dto.advisor;

import app.vetra.ai.entity.AIAdvisorSenderType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Response representation of a message turn in an AI Veterinary Advisor session. */
public record AIAdvisorMessageResponse(
    UUID id,
    AIAdvisorSenderType senderType,
    String content,
    int turnNumber,
    List<String> followUpQuestions,
    AIAdvisorAssessmentDTO assessment,
    Instant createdAt
) {}
