package app.vetra.ai.provider;

import app.vetra.ai.entity.AIProviderType;
import java.math.BigDecimal;

/**
 * Diagnostic analysis result emitted by an AI provider implementation.
 *
 * @param diagnosis diagnostic text or disease classification
 * @param confidenceScore model confidence score between 0.000 and 1.000
 * @param providerType AI provider type enum
 * @param modelName specific model checkpoint or identifier used
 */
public record AIProviderResult(
    String diagnosis,
    BigDecimal confidenceScore,
    AIProviderType providerType,
    String modelName
) {}
