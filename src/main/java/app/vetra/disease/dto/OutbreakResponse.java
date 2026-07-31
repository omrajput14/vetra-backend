package app.vetra.disease.dto;

import app.vetra.disease.entity.Outbreak;
import app.vetra.disease.entity.OutbreakStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Public DTO representing an outbreak cluster entity.
 */
public record OutbreakResponse(
    UUID id,
    String diseaseName,
    String severity,
    OutbreakStatus status,
    Double centerLatitude,
    Double centerLongitude,
    Double radiusKm,
    Integer affectedReportsCount,
    Instant createdAt,
    Instant updatedAt
) {

  /**
   * Factory method mapping Outbreak entity to OutbreakResponse DTO.
   *
   * @param outbreak entity instance
   * @return {@link OutbreakResponse} DTO
   */
  public static OutbreakResponse fromEntity(Outbreak outbreak) {
    return new OutbreakResponse(
        outbreak.getId(),
        outbreak.getDiseaseName(),
        outbreak.getSeverity(),
        outbreak.getStatus(),
        outbreak.getCenterLatitude(),
        outbreak.getCenterLongitude(),
        outbreak.getRadiusKm(),
        outbreak.getAffectedReportsCount(),
        outbreak.getCreatedAt(),
        outbreak.getUpdatedAt()
    );
  }
}
