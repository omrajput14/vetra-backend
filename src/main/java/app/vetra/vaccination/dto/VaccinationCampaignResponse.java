package app.vetra.vaccination.dto;

import app.vetra.vaccination.entity.VaccinationCampaign;
import app.vetra.vaccination.enums.CampaignPriority;
import app.vetra.vaccination.enums.CampaignStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Public response DTO for a vaccination campaign record.
 */
public record VaccinationCampaignResponse(
    UUID id,
    String campaignName,
    String diseaseName,
    String targetDistrict,
    String targetTaluka,
    int targetLivestockCount,
    int plannedDoses,
    int administeredDoses,
    double coverageProgressPercentage,
    CampaignPriority priority,
    CampaignStatus status,
    LocalDate startDate,
    LocalDate endDate,
    UUID outbreakId,
    UUID createdById,
    String createdByName,
    String notes,
    Instant createdAt,
    Instant updatedAt) {

  /**
   * Factory mapping method from {@link VaccinationCampaign} entity.
   *
   * @param entity persistent campaign entity
   * @return {@link VaccinationCampaignResponse}
   */
  public static VaccinationCampaignResponse fromEntity(VaccinationCampaign entity) {
    UUID outbreakId = entity.getOutbreak() != null ? entity.getOutbreak().getId() : null;
    UUID createdById = entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null;
    String createdByName = null;
    if (entity.getCreatedBy() != null) {
      createdByName = entity.getCreatedBy().getEmail() != null
          ? entity.getCreatedBy().getEmail()
          : entity.getCreatedBy().getPhone();
    }

    return new VaccinationCampaignResponse(
        entity.getId(),
        entity.getCampaignName(),
        entity.getDiseaseName(),
        entity.getTargetDistrict(),
        entity.getTargetTaluka(),
        entity.getTargetLivestockCount(),
        entity.getPlannedDoses(),
        entity.getAdministeredDoses(),
        entity.getCoverageProgressPercentage(),
        entity.getPriority(),
        entity.getStatus(),
        entity.getStartDate(),
        entity.getEndDate(),
        outbreakId,
        createdById,
        createdByName,
        entity.getNotes(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
