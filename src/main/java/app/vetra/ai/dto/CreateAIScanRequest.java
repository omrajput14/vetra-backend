package app.vetra.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request payload for submitting a new livestock image scan for AI diagnosis.
 *
 * @param animalId target animal UUID
 * @param imageUrl public or S3 URL of the uploaded livestock image
 * @param imageHash optional SHA-256 hash of the image content for deduplication
 */
public record CreateAIScanRequest(
    @NotNull(message = "Animal ID is required") UUID animalId,
    @NotBlank(message = "Image URL is required") String imageUrl,
    String imageHash) {}
