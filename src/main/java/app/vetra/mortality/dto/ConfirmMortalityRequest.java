package app.vetra.mortality.dto;

import app.vetra.mortality.enums.MortalityCauseCategory;
import jakarta.validation.constraints.NotNull;

/** DTO payload submitted by a veterinarian to confirm an animal mortality report. */
public record ConfirmMortalityRequest(
    @NotNull(message = "Cause category is required") MortalityCauseCategory causeCategory,
    String diseaseName,
    String clinicalNotes,
    Boolean postMortemConducted) {}
