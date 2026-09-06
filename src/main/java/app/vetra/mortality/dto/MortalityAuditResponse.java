package app.vetra.mortality.dto;

import app.vetra.mortality.entity.MortalityReviewAudit;
import app.vetra.mortality.enums.MortalityCauseCategory;
import app.vetra.mortality.enums.MortalityReportStatus;
import java.time.Instant;
import java.util.UUID;

/** Response DTO representing an immutable veterinarian mortality review audit entry. */
public record MortalityAuditResponse(
    UUID id,
    UUID mortalityEventId,
    UUID veterinarianId,
    String veterinarianName,
    String action,
    MortalityReportStatus oldStatus,
    MortalityReportStatus newStatus,
    String clinicalNotes,
    MortalityCauseCategory confirmedCauseCategory,
    String confirmedDiseaseName,
    Instant createdAt) {

  /** Factory method converting a MortalityReviewAudit entity to MortalityAuditResponse. */
  public static MortalityAuditResponse fromEntity(MortalityReviewAudit audit) {
    return new MortalityAuditResponse(
        audit.getId(),
        audit.getMortalityEvent().getId(),
        audit.getVeterinarian().getId(),
        audit.getVeterinarian().getFullName(),
        audit.getAction(),
        audit.getOldStatus(),
        audit.getNewStatus(),
        audit.getClinicalNotes(),
        audit.getConfirmedCauseCategory(),
        audit.getConfirmedDiseaseName(),
        audit.getCreatedAt());
  }
}
