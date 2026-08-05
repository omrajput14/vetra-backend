package app.vetra.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for rejecting an AI diagnostic scan result.
 *
 * @param rejectionReason mandatory reason for rejecting the AI output
 */
public record RejectAIScanRequest(
    @NotBlank(message = "Rejection reason is required")
        @Size(max = 5000, message = "Rejection reason cannot exceed 5000 characters")
        String rejectionReason) {}
