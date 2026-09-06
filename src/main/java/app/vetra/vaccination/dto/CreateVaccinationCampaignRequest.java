package app.vetra.vaccination.dto;

import app.vetra.vaccination.enums.CampaignPriority;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request payload for launching an official vaccination campaign.
 */
public record CreateVaccinationCampaignRequest(
    @NotBlank(message = "Campaign name is required")
    @Size(max = 150, message = "Campaign name cannot exceed 150 characters")
    String campaignName,

    @NotBlank(message = "Disease name is required")
    @Size(max = 128, message = "Disease name cannot exceed 128 characters")
    String diseaseName,

    @NotBlank(message = "Target district is required")
    @Size(max = 100, message = "Target district cannot exceed 100 characters")
    String targetDistrict,

    @Size(max = 100, message = "Target taluka cannot exceed 100 characters")
    String targetTaluka,

    @Min(value = 0, message = "Target livestock count must be non-negative")
    Integer targetLivestockCount,

    @NotNull(message = "Planned doses is required")
    @Min(value = 1, message = "Planned doses must be at least 1")
    Integer plannedDoses,

    CampaignPriority priority,

    @NotNull(message = "Start date is required")
    LocalDate startDate,

    LocalDate endDate,

    UUID outbreakId,

    @Size(max = 5000, message = "Notes cannot exceed 5000 characters")
    String notes) {}
