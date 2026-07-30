package app.vetra.ai.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request payload for a licensed veterinarian to verify or correct an AI diagnostic result.
 *
 * @param acceptDiagnosis true if vet accepts AI diagnosis, false if rejecting/correcting
 * @param veterinarianNotes clinical notes or feedback on the AI recommendation
 * @param correctedDiagnosis human-corrected diagnosis if AI recommendation was inaccurate
 */
public record VerifyAIScanRequest(
    @NotNull(message = "Verification decision is required")
    Boolean acceptDiagnosis,

    String veterinarianNotes,

    String correctedDiagnosis
) {}
