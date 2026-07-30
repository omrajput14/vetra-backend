package app.vetra.ai.dto;

import jakarta.validation.constraints.Size;

/**
 * Request payload for approving an AI diagnostic scan and creating an EVMR entry.
 *
 * @param notes optional veterinarian verification notes
 * @param customDiagnosis optional clinical diagnosis override by veterinarian
 * @param treatmentNotes optional recommended treatment plan
 */
public record ApproveAIScanRequest(
    @Size(max = 5000, message = "Notes cannot exceed 5000 characters")
    String notes,

    @Size(max = 255, message = "Custom diagnosis cannot exceed 255 characters")
    String customDiagnosis,

    @Size(max = 5000, message = "Treatment notes cannot exceed 5000 characters")
    String treatmentNotes
) {}
