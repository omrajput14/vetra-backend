package app.vetra.ai.dto;

import app.vetra.ai.entity.AIProviderType;
import app.vetra.ai.entity.AIScan;
import app.vetra.ai.entity.AIScanStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Standard API response representation of an AI Diagnostic Scan. */
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
    String severity,
    List<String> observations,
    String recommendedNextStep,
    Boolean requiresVeterinarianReview,
    String disclaimer,
    AIScanStatus status,
    boolean veterinarianVerified,
    UUID verifiedByUserId,
    String verifiedByVetName,
    Instant verifiedAt,
    String notes,
    Instant createdAt,
    Instant updatedAt) {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * Factory mapper converting an AIScan JPA entity to AIScanResponse record.
   *
   * @param scan source AIScan entity
   * @return populated AIScanResponse instance
   */
  public static AIScanResponse fromEntity(AIScan scan) {
    String severity = "UNKNOWN";
    List<String> observations = new ArrayList<>();
    String recommendedNextStep =
        "Schedule a clinical evaluation with a licensed veterinarian for on-site diagnosis.";
    Boolean requiresVeterinarianReview = true;
    String disclaimer =
        "This is an AI-assisted preliminary assessment and is not a confirmed veterinary diagnosis.";

    if (scan.getNotes() != null && scan.getNotes().trim().startsWith("{")) {
      try {
        JsonNode root = MAPPER.readTree(scan.getNotes());
        if (root.hasNonNull("severity")) {
          severity = root.get("severity").asText();
        }
        if (root.has("observations") && root.get("observations").isArray()) {
          for (JsonNode obs : root.get("observations")) {
            observations.add(obs.asText());
          }
        }
        if (root.hasNonNull("recommendedNextStep")) {
          recommendedNextStep = root.get("recommendedNextStep").asText();
        }
        if (root.hasNonNull("requiresVeterinarianReview")) {
          requiresVeterinarianReview = root.get("requiresVeterinarianReview").asBoolean();
        }
        if (root.hasNonNull("disclaimer")) {
          disclaimer = root.get("disclaimer").asText();
        }
      } catch (Exception ignored) {
        // preserve defaults if notes is not valid JSON
      }
    }

    String uploader =
        scan.getUploadedBy().getEmail() != null
            ? scan.getUploadedBy().getEmail()
            : scan.getUploadedBy().getPhone();

    UUID verifierId = scan.getVerifiedBy() != null ? scan.getVerifiedBy().getId() : null;
    String verifierName = null;
    if (scan.getVerifiedBy() != null) {
      verifierName =
          scan.getVerifiedBy().getEmail() != null
              ? scan.getVerifiedBy().getEmail()
              : scan.getVerifiedBy().getPhone();
    }

    return new AIScanResponse(
        scan.getId(),
        scan.getAnimal().getId(),
        scan.getAnimal().getAnimalName(),
        scan.getUploadedBy().getId(),
        uploader,
        scan.getImageUrl(),
        scan.getImageHash(),
        scan.getAiProvider(),
        scan.getAiModel(),
        scan.getDiagnosis(),
        scan.getConfidenceScore(),
        severity,
        observations,
        recommendedNextStep,
        requiresVeterinarianReview,
        disclaimer,
        scan.getStatus(),
        scan.isVeterinarianVerified(),
        verifierId,
        verifierName,
        scan.getVerifiedAt(),
        scan.getNotes(),
        scan.getCreatedAt(),
        scan.getUpdatedAt());
  }
}
