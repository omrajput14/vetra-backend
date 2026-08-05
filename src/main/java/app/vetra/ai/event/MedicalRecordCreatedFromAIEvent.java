package app.vetra.ai.event;

import java.util.UUID;

/**
 * Event published when an immutable MedicalRecord entry is automatically created from an approved
 * AI scan.
 *
 * @param medicalRecordId created medical record UUID
 * @param scanId verified AI scan UUID
 * @param animalId target animal UUID
 * @param vetId approving veterinarian user UUID
 */
public record MedicalRecordCreatedFromAIEvent(
    UUID medicalRecordId, UUID scanId, UUID animalId, UUID vetId) {}
