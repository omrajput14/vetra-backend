package app.vetra.ai.event;

import java.util.UUID;

/**
 * Event published when a new AI diagnostic scan request is submitted.
 *
 * @param scanId scan UUID
 * @param animalId target animal UUID
 * @param imageUrl image URL to analyze
 * @param uploadedBy uploader user UUID
 */
public record AIScanCreatedEvent(UUID scanId, UUID animalId, String imageUrl, UUID uploadedBy) {}
