package app.vetra.ai.event;

import java.util.UUID;

/**
 * Event published when a licensed veterinarian rejects an AI diagnostic scan result.
 *
 * @param scanId scan UUID
 * @param rejectionReason rejection reason or notes
 * @param rejectedBy veterinarian user UUID
 */
public record AIScanRejectedEvent(
    UUID scanId,
    String rejectionReason,
    UUID rejectedBy
) {}
