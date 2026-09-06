package app.vetra.mortality.dto;

import jakarta.validation.constraints.NotBlank;

/** DTO payload submitted by a veterinarian to reject an animal mortality report. */
public record RejectMortalityRequest(
    @NotBlank(message = "Rejection reason is required") String rejectionReason,
    String clinicalNotes) {}
