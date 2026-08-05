package app.vetra.ai.event;

import app.vetra.ai.provider.AIInferenceResult;
import java.util.UUID;

/**
 * Event published when an AI provider inference completes successfully.
 *
 * @param scanId scan UUID
 * @param result inference result payload
 */
public record AIInferenceCompletedEvent(UUID scanId, AIInferenceResult result) {}
