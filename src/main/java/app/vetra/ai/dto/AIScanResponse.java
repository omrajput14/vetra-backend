package app.vetra.ai.dto;

import app.vetra.ai.entity.AIScan;
import app.vetra.ai.entity.AIProviderType;
import app.vetra.ai.entity.AIScanStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Standard API response representation of an AI Diagnostic Scan.
 */
public record AIScanResponse(
    UUID id,
    UUID animalId,
    String animalName,
    UUID uploadedByUserId,
    String uploadedByUserName,
    String imageUrl,
    String imageHash,
    AIProviderType aiProvider,
    String aiModel,
    String diagnosis,
    BigDecimal confidenceScore,
    AIScanStatus status,
    boolean veterinarianVerified,
    UUID verifiedByUserId,
    String verifiedByVetName,
    Instant verifiedAt,
    String notes,
    Instant createdAt,
    Instant updatedAt
) {

  /**
   * Factory mapper converting an AIScan JPA entity to AIScanResponse record.
   *
   * @param scan source AIScan entity
   * @return populated AIScanResponse instance
   */
  public static AIScanResponse fromEntity(AIScan scan) {
    return new AIScanResponse(
        scan.getId(),
        scan.getAnimal().getId(),
        scan.getAnimal().getAnimalName(),
        scan.getUploadedBy().getId(),
        scan.getUploadedBy().getEmail() != null
            ? scan.getUploadedBy().getEmail()
            : scan.getUploadedBy().getPhone(),
        scan.getImageUrl(),
        scan.getImageHash(),
        scan.getAiProvider(),
        scan.getAiModel(),
        scan.getDiagnosis(),
        scan.getConfidenceScore(),
        scan.getStatus(),
        scan.isVeterinarianVerified(),
        scan.getVerifiedBy() != null ? scan.getVerifiedBy().getId() : null,
        scan.getVerifiedBy() != null
            ? (scan.getVerifiedBy().getEmail() != null
                ? scan.getVerifiedBy().getEmail()
                : scan.getVerifiedBy().getPhone())
            : null,
        scan.getVerifiedAt(),
        scan.getNotes(),
        scan.getCreatedAt(),
        scan.getUpdatedAt()
    );
  }
}
