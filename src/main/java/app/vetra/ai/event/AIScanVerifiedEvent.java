package app.vetra.ai.event;

import java.util.UUID;

/**
 * Event published when a licensed veterinarian completes verification of an AI diagnostic scan.
 *
 * @param scanId scan UUID
 * @param accepted true if vet accepted AI diagnosis, false if rejected/corrected
 * @param verifiedBy veterinarian user UUID
 */
public record AIScanVerifiedEvent(UUID scanId, boolean accepted, UUID verifiedBy) {}
