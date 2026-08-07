package app.vetra.ai.event;

import java.util.UUID;

/**
 * Domain event published when an AI diagnostic inference completes successfully.
 *
 * <p>This event carries the scanId allowing asynchronous consumers (like Notification)
 * to react without coupling to AI provider details.
 */
public record AIInferenceCompletedEvent(UUID scanId) {}
