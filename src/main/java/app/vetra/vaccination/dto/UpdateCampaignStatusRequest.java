package app.vetra.vaccination.dto;

import app.vetra.vaccination.enums.CampaignStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for transitioning a campaign's lifecycle status.
 */
public record UpdateCampaignStatusRequest(
    @NotNull(message = "New campaign status is required")
    CampaignStatus status,

    @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
    String notes) {}
