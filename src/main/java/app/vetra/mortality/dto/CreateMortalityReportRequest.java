package app.vetra.mortality.dto;

import app.vetra.mortality.enums.MortalityCauseCategory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/** Request DTO for submitting an animal mortality report. */
public record CreateMortalityReportRequest(
    @NotNull(message = "Animal ID is required")
    UUID animalId,

    @NotNull(message = "Cause category is required")
    MortalityCauseCategory causeCategory,

    @Size(max = 2000, message = "Cause description cannot exceed 2000 characters")
    String causeDescription,

    @Size(max = 128, message = "Disease name cannot exceed 128 characters")
    String diseaseName,

    Boolean recentlyTreated,

    @Size(max = 2000, message = "Treatment notes cannot exceed 2000 characters")
    String treatmentNotes,

    @Size(max = 5000, message = "Notes cannot exceed 5000 characters")
    String notes,

    Instant deathDateTime,

    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90.0")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90.0")
    Double latitude,

    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180.0")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180.0")
    Double longitude,

    @DecimalMin(value = "0.0", message = "Location accuracy must be non-negative")
    Double locationAccuracy) {}
