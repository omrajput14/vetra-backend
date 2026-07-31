package app.vetra.ai.provider.gemini;

import app.vetra.ai.entity.AIProviderType;
import app.vetra.ai.exception.AIInferenceException;
import app.vetra.ai.provider.AIInferenceResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Mapper converting raw Gemini Vision API JSON payloads into enterprise AIInferenceResult records.
 */
@Component
public class GeminiResponseMapper {

  private static final Logger log = LoggerFactory.getLogger(GeminiResponseMapper.class);
  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Maps Gemini JSON response text into standardized {@link AIInferenceResult}.
   *
   * @param rawResponse raw response text string from Gemini API
   * @param modelName active Gemini model name
   * @param requestId correlation request ID
   * @param latencyMs execution duration in milliseconds
   * @return {@link AIInferenceResult} standardized DTO
   */
  public AIInferenceResult mapToInferenceResult(
      String rawResponse, String modelName, String requestId, long latencyMs) {

    try {
      String cleanJson = cleanMarkdownJson(rawResponse);
      GeminiDiagnosticPayload payload = objectMapper.readValue(cleanJson, GeminiDiagnosticPayload.class);

      String diagnosis = payload.condition() != null ? payload.condition() : "Unspecified Observation";
      if (payload.observations() != null && !payload.observations().isEmpty()) {
        diagnosis += " | Observations: " + String.join(", ", payload.observations());
      }

      BigDecimal confidence = payload.confidence() != null ? payload.confidence() : BigDecimal.valueOf(0.50);

      List<String> warnings = new ArrayList<>();
      if (payload.recommendations() != null && !payload.recommendations().isEmpty()) {
        warnings.add("Recommendations: " + String.join("; ", payload.recommendations()));
      }
      if (Boolean.TRUE.equals(payload.requiresVeterinarianReview())) {
        warnings.add("Requires urgent veterinarian review");
      }

      return new AIInferenceResult(
          AIProviderType.GEMINI,
          modelName,
          diagnosis,
          confidence,
          rawResponse,
          requestId,
          latencyMs,
          null,
          warnings,
          Instant.now()
      );

    } catch (Exception ex) {
      log.error("Failed to parse Gemini Vision API JSON response: {}", ex.getMessage());
      throw new AIInferenceException("Failed to parse Gemini Vision response payload: " + ex.getMessage(), "AI_004");
    }
  }

  private String cleanMarkdownJson(String text) {
    if (text == null) {
      return "{}";
    }
    String cleaned = text.trim();
    if (cleaned.startsWith("```json")) {
      cleaned = cleaned.substring(7);
    } else if (cleaned.startsWith("```")) {
      cleaned = cleaned.substring(3);
    }
    if (cleaned.endsWith("```")) {
      cleaned = cleaned.substring(0, cleaned.length() - 3);
    }
    return cleaned.trim();
  }
}
