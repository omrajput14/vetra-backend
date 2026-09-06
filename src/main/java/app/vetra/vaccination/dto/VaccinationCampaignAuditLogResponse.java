package app.vetra.vaccination.dto;

import app.vetra.vaccination.entity.VaccinationCampaignAuditLog;
import java.time.Instant;
import java.util.UUID;

/**
 * Audit log entry response for a vaccination campaign.
 */
public record VaccinationCampaignAuditLogResponse(
    UUID id,
    UUID campaignId,
    UUID performedById,
    String performedByName,
    String action,
    String previousStatus,
    String newStatus,
    String notes,
    Instant createdAt) {

  /**
   * Maps entity to response DTO.
   *
   * @param log audit log entity
   * @return {@link VaccinationCampaignAuditLogResponse}
   */
  public static VaccinationCampaignAuditLogResponse fromEntity(VaccinationCampaignAuditLog log) {
    String officerName = null;
    if (log.getPerformedBy() != null) {
      officerName = log.getPerformedBy().getEmail() != null
          ? log.getPerformedBy().getEmail()
          : log.getPerformedBy().getPhone();
    }
    return new VaccinationCampaignAuditLogResponse(
        log.getId(),
        log.getCampaign().getId(),
        log.getPerformedBy().getId(),
        officerName,
        log.getAction(),
        log.getPreviousStatus(),
        log.getNewStatus(),
        log.getNotes(),
        log.getCreatedAt());
  }
}
