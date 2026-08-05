package app.vetra.ai.event;

import app.vetra.ai.entity.AIProviderType;
import java.util.UUID;

/**
 * Event published when an AI provider inference fails after retries.
 *
 * @param scanId scan UUID
 * @param errorMessage error message
 * @param provider attempted provider type
 */
public record AIInferenceFailedEvent(UUID scanId, String errorMessage, AIProviderType provider) {}
