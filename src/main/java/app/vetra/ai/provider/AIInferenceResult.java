package app.vetra.ai.provider;

import app.vetra.ai.entity.AIProviderType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Enterprise inference result DTO returned by an AI provider implementation.
 *
 * @param provider provider engine type
 * @param model model name or checkpoint identifier
 * @param diagnosis diagnostic text classification or analysis
 * @param confidence confidence score between 0.000 and 1.000
 * @param rawResponse raw JSON string payload returned by model endpoint
 * @param requestId correlation request ID
 * @param latencyMs execution duration in milliseconds
 * @param tokensUsed prompt/completion token usage count if applicable
 * @param warnings list of warning messages returned by provider
 * @param createdAt result timestamp
 */
public record AIInferenceResult(
    AIProviderType provider,
    String model,
    String diagnosis,
    BigDecimal confidence,
    String rawResponse,
    String requestId,
    long latencyMs,
    Integer tokensUsed,
    List<String> warnings,
    Instant createdAt
) {}
