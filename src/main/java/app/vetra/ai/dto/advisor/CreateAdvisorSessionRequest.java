package app.vetra.ai.dto.advisor;

import jakarta.validation.constraints.Size;

/** Request payload for initializing a new AI Veterinary Advisor session. */
public record CreateAdvisorSessionRequest(
    @Size(max = 2000, message = "Initial message must not exceed 2000 characters")
    String initialMessage
) {}
