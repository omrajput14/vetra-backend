package app.vetra.ai.provider.gemini;

import app.vetra.ai.exception.AIAuthenticationException;
import app.vetra.ai.exception.AIException;
import app.vetra.ai.exception.AIInferenceException;
import app.vetra.ai.exception.AIProviderUnavailableException;
import app.vetra.ai.exception.AIRateLimitException;
import app.vetra.ai.exception.AITimeoutException;
import app.vetra.ai.exception.AITokenLimitExceededException;
import app.vetra.ai.model.AIRequest;
import app.vetra.ai.model.AIResponse;
import app.vetra.ai.provider.AIProvider;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Gemini Vision AI Provider strategy implementing visual diagnostic inference via Google Gemini
 * REST API.
 */
@Component("geminiAIProvider")
public class GeminiAIProvider implements AIProvider {

  private static final Logger log = LoggerFactory.getLogger(GeminiAIProvider.class);

  private final GeminiProperties properties;
  private final WebClient webClient;

  /**
   * Constructor injection for GeminiAIProvider.
   *
   * @param properties configuration properties
   * @param webClientBuilder Spring WebClient builder
   */
  public GeminiAIProvider(
      GeminiProperties properties,
      WebClient.Builder webClientBuilder) {
    this.properties = properties;
    this.webClient = webClientBuilder.baseUrl(properties.getBaseUrl()).build();
  }

  @Override
  public AIResponse execute(AIRequest request, String promptText) {
    if (!isAvailable()) {
      throw new AIProviderUnavailableException(
          "Gemini Vision provider is disabled or missing API key configuration", "AI_003");
    }

    String requestId = "GEM-" + UUID.randomUUID().toString().substring(0, 8);
    log.info(
        "GeminiAIProvider executing promptId={} model={}",
        request.promptId(),
        properties.getModel());

    try {
      Map<String, Object> requestPayload = buildRequestPayload(request, promptText);
      Duration timeout = properties.getTimeout() != null
          ? properties.getTimeout()
          : Duration.ofSeconds(30);

      String rawResponse =
          webClient
              .post()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path("/v1beta/models/" + properties.getModel() + ":generateContent")
                          .queryParam("key", properties.getApiKey())
                          .build())
              .contentType(MediaType.APPLICATION_JSON)
              .bodyValue(requestPayload)
              .retrieve()
              .bodyToMono(String.class)
              .block(timeout);

      return new AIResponse(
          extractCandidateText(rawResponse),
          "v1",
          providerName(),
          properties.getModel(),
          extractPromptTokens(rawResponse),
          extractCompletionTokens(rawResponse),
          extractFinishReason(rawResponse));

    } catch (WebClientResponseException ex) {
      throw handleWebClientException(ex, requestId);
    } catch (Exception ex) {
      throw handleGenericException(ex, requestId);
    }
  }

  private Map<String, Object> buildRequestPayload(AIRequest request, String promptText) {
    List<Map<String, Object>> parts = new ArrayList<>();
    String finalPrompt = promptText;

    if (request.imageUrl() != null && !request.imageUrl().isBlank()) {
      String img = request.imageUrl().trim();
      if (img.startsWith("data:") && img.contains(";base64,")) {
        int colonIdx = img.indexOf(':');
        int semiIdx = img.indexOf(";base64,");
        String mimeType = img.substring(colonIdx + 1, semiIdx);
        String base64Data = img.substring(semiIdx + 8);
        parts.add(Map.of("inline_data", Map.of("mime_type", mimeType, "data", base64Data)));
      } else {
        finalPrompt = promptText + "\nAnalyze image at URL: " + img;
      }
    }
    parts.add(Map.of("text", finalPrompt));
    return Map.of("contents", List.of(Map.of("parts", parts)));
  }

  private RuntimeException handleWebClientException(WebClientResponseException ex, String reqId) {
    log.error("Gemini API HTTP error reqId={} status={} body={}", reqId, ex.getStatusCode(),
        ex.getResponseBodyAsString());
    if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED
        || ex.getStatusCode() == HttpStatus.FORBIDDEN) {
      return new AIAuthenticationException("Invalid or unauthorized Gemini API key", providerName());
    } else if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
      return new AIRateLimitException("Gemini API rate limit exceeded", providerName());
    } else if (ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
      return new AITokenLimitExceededException(
          "Gemini API request payload exceeded token limits", providerName());
    } else if (ex.getStatusCode().is5xxServerError()) {
      return new AIProviderUnavailableException(
          "Gemini API server error: " + ex.getStatusCode(), providerName());
    }
    return new AIInferenceException("Gemini API error: " + ex.getStatusCode(), providerName());
  }

  private RuntimeException handleGenericException(Exception ex, String requestId) {
    log.error("Gemini API request failed reqId={} error={}", requestId, ex.getMessage());
    if (ex instanceof AIException aie) {
      return aie;
    }
    if (ex instanceof java.util.concurrent.TimeoutException
        || (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("timeout"))) {
      return new AITimeoutException("Gemini Vision API request timed out", providerName());
    }
    return new AIInferenceException("Gemini Vision API failed: " + ex.getMessage(), providerName());
  }

  @Override
  public boolean health() {
    return isAvailable();
  }

  @Override
  public String providerName() {
    return "GEMINI";
  }

  @Override
  public boolean isAvailable() {
    return properties.isEnabled()
        && properties.getApiKey() != null
        && !properties.getApiKey().isBlank();
  }

  private String extractCandidateText(String rawResponse) {
    if (rawResponse == null) {
      return "{}";
    }
    try {
      com.fasterxml.jackson.databind.JsonNode root =
          new com.fasterxml.jackson.databind.ObjectMapper().readTree(rawResponse);
      com.fasterxml.jackson.databind.JsonNode candidates = root.path("candidates");
      if (candidates.isArray() && !candidates.isEmpty()) {
        com.fasterxml.jackson.databind.JsonNode parts =
            candidates.get(0).path("content").path("parts");
        if (parts.isArray() && !parts.isEmpty()) {
          return parts.get(0).path("text").asText();
        }
      }
    } catch (Exception ex) {
      log.warn("Failed to extract candidate text: {}", ex.getMessage());
    }
    return rawResponse;
  }

  private int extractPromptTokens(String rawResponse) {
    if (rawResponse == null) {
      return 0;
    }
    try {
      com.fasterxml.jackson.databind.JsonNode root =
          new com.fasterxml.jackson.databind.ObjectMapper().readTree(rawResponse);
      return root.path("usageMetadata").path("promptTokenCount").asInt(0);
    } catch (Exception ex) {
      return 0;
    }
  }

  private int extractCompletionTokens(String rawResponse) {
    if (rawResponse == null) {
      return 0;
    }
    try {
      com.fasterxml.jackson.databind.JsonNode root =
          new com.fasterxml.jackson.databind.ObjectMapper().readTree(rawResponse);
      return root.path("usageMetadata").path("candidatesTokenCount").asInt(0);
    } catch (Exception ex) {
      return 0;
    }
  }

  private String extractFinishReason(String rawResponse) {
    if (rawResponse == null) {
      return "unknown";
    }
    try {
      com.fasterxml.jackson.databind.JsonNode root =
          new com.fasterxml.jackson.databind.ObjectMapper().readTree(rawResponse);
      com.fasterxml.jackson.databind.JsonNode candidates = root.path("candidates");
      if (candidates.isArray() && !candidates.isEmpty()) {
        return candidates.get(0).path("finishReason").asText("unknown");
      }
    } catch (Exception ex) {
      // ignore parsing errors and return unknown
    }
    return "unknown";
  }
}
