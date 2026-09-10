package app.vetra.disease.dto;

import app.vetra.ai.entity.AIScan;
import app.vetra.ai.entity.AIScanStatus;
import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.entity.FarmerProfile;
import app.vetra.infrastructure.persistence.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Public surveillance DTO representing an AI livestock image preliminary screening scan.
 *
 * <p>Used by government surveillance officers for early-warning monitoring. An AI screening
 * scan is preliminary and must not be treated as a confirmed diagnosis or outbreak cluster.
 */
public record AIScreeningResponse(
    UUID id,
    UUID animalId,
    String tagNumber,
    String animalName,
    String species,
    String preliminaryDiagnosis,
    BigDecimal confidenceScore,
    String severity,
    AIScanStatus status,
    boolean veterinarianVerified,
    UUID verifiedByUserId,
    String verifiedByVetName,
    Instant verifiedAt,
    String source,
    Double latitude,
    Double longitude,
    String district,
    String taluka,
    String state,
    String imageUrl,
    Instant createdAt,
    Instant updatedAt) {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String DEFAULT_DIAGNOSIS = "Preliminary result unavailable";
  private static final String DEFAULT_SEVERITY = "UNKNOWN";

  /**
   * Factory method mapping an AIScan entity and associated location data to an AIScreeningResponse.
   * Strips large base64 image payloads for lightweight list transmission.
   *
   * @param scan source AIScan JPA entity
   * @return populated AIScreeningResponse DTO with lightweight image reference
   */
  public static AIScreeningResponse fromEntity(AIScan scan) {
    return fromEntity(scan, false);
  }

  /**
   * Factory method mapping an AIScan entity with explicit control over whether raw/large base64
   * image data is included.
   *
   * @param scan source AIScan JPA entity
   * @param includeFullImage whether to preserve full base64 data URIs
   * @return populated AIScreeningResponse DTO
   */
  public static AIScreeningResponse fromEntity(AIScan scan, boolean includeFullImage) {
    Animal animal = scan.getAnimal();
    FarmerProfile farmer = animal != null ? animal.getFarmer() : null;

    return new AIScreeningResponse(
        scan.getId(),
        animal != null ? animal.getId() : null,
        animal != null ? animal.getTagNumber() : null,
        animal != null ? animal.getAnimalName() : null,
        resolveSpecies(animal),
        resolveDiagnosis(scan.getDiagnosis()),
        scan.getConfidenceScore(),
        resolveSeverity(scan.getNotes()),
        scan.getStatus(),
        scan.isVeterinarianVerified(),
        scan.getVerifiedBy() != null ? scan.getVerifiedBy().getId() : null,
        resolveVerifierName(scan.getVerifiedBy()),
        scan.getVerifiedAt(),
        "AI_PRELIMINARY_SCREENING",
        farmer != null ? farmer.getLatitude() : null,
        farmer != null ? farmer.getLongitude() : null,
        farmer != null ? farmer.getDistrict() : null,
        farmer != null ? farmer.getTaluka() : null,
        farmer != null ? farmer.getState() : null,
        resolveImageUrl(scan, includeFullImage),
        scan.getCreatedAt(),
        scan.getUpdatedAt());
  }

  private static String resolveImageUrl(AIScan scan, boolean includeFullImage) {
    String url = scan.getImageUrl();
    if (url == null || url.isBlank()) {
      return null;
    }
    if (includeFullImage) {
      return url;
    }
    if (url.startsWith("data:image/") || url.length() > 512) {
      return "/api/v1/disease/ai-screenings/" + scan.getId() + "/image";
    }
    return url;
  }

  private static String resolveSpecies(Animal animal) {
    if (animal == null || animal.getSpecies() == null) {
      return null;
    }
    return animal.getSpecies().name();
  }

  private static String resolveDiagnosis(String diagnosis) {
    if (diagnosis == null || diagnosis.isBlank()) {
      return DEFAULT_DIAGNOSIS;
    }
    return diagnosis.trim();
  }

  private static String resolveSeverity(String notes) {
    if (notes == null || !notes.trim().startsWith("{")) {
      return DEFAULT_SEVERITY;
    }
    try {
      JsonNode root = MAPPER.readTree(notes);
      if (root.hasNonNull("severity")) {
        return root.get("severity").asText();
      }
    } catch (Exception ignored) {
      // Return default UNKNOWN on parsing exception
    }
    return DEFAULT_SEVERITY;
  }

  private static String resolveVerifierName(User verifier) {
    if (verifier == null) {
      return null;
    }
    if (verifier.getEmail() != null && !verifier.getEmail().isBlank()) {
      return verifier.getEmail();
    }
    return verifier.getPhone();
  }
}
