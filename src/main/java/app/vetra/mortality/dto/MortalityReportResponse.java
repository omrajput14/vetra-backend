package app.vetra.mortality.dto;

import app.vetra.mortality.entity.AnimalMortalityEvent;
import app.vetra.mortality.enums.MortalityCauseCategory;
import app.vetra.mortality.enums.MortalityReportStatus;
import app.vetra.mortality.enums.MortalitySource;
import java.time.Instant;
import java.util.UUID;

/** Public response DTO representing a recorded animal mortality event. */
public record MortalityReportResponse(
    UUID id,
    UUID animalId,
    String animalName,
    String tagNumber,
    String qrCodeId,
    UUID farmerId,
    String farmerName,
    UUID reportedById,
    MortalityCauseCategory causeCategory,
    String causeDescription,
    String diseaseName,
    boolean recentlyTreated,
    String treatmentNotes,
    String notes,
    MortalitySource source,
    MortalityReportStatus status,
    Instant deathDateTime,
    Instant reportedAt,
    Double latitude,
    Double longitude,
    Double locationAccuracy,
    UUID vetReviewedById,
    String vetReviewedByName,
    Instant vetReviewedAt,
    MortalityCauseCategory vetCauseCategory,
    String vetDiseaseName,
    String vetClinicalNotes,
    String vetRejectionReason,
    boolean postMortemConducted,
    Instant createdAt,
    Instant updatedAt) {

  /**
   * Factory method mapping AnimalMortalityEvent entity to MortalityReportResponse DTO.
   */
  public static MortalityReportResponse fromEntity(AnimalMortalityEvent event) {
    UUID vetId = event.getVetReviewedBy() != null ? event.getVetReviewedBy().getId() : null;
    String vetName = event.getVetReviewedBy() != null ? event.getVetReviewedBy().getFullName() : null;

    return new MortalityReportResponse(
        event.getId(),
        event.getAnimal().getId(),
        event.getAnimal().getAnimalName(),
        event.getTagNumber(),
        event.getQrCodeId(),
        event.getFarmer().getId(),
        event.getFarmer().getFullName(),
        event.getReportedBy().getId(),
        event.getCauseCategory(),
        event.getCauseDescription(),
        event.getDiseaseName(),
        event.isRecentlyTreated(),
        event.getTreatmentNotes(),
        event.getNotes(),
        event.getSource(),
        event.getStatus(),
        event.getDeathDateTime(),
        event.getReportedAt(),
        event.getLatitude(),
        event.getLongitude(),
        event.getLocationAccuracy(),
        vetId,
        vetName,
        event.getVetReviewedAt(),
        event.getVetCauseCategory(),
        event.getVetDiseaseName(),
        event.getVetClinicalNotes(),
        event.getVetRejectionReason(),
        event.isPostMortemConducted(),
        event.getCreatedAt(),
        event.getUpdatedAt());
  }
}
