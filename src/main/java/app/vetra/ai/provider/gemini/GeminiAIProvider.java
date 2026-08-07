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

    long startTime = System.currentTimeMillis();
    String requestId = "GEM-" + UUID.randomUUID().toString().substring(0, 8);

    log.info(
        "GeminiAIProvider executing promptId={} model={}",
        request.promptId(),
        properties.getModel());

    try {
      // Build request body manually for execute
      Map<String, Object> textPart =
          Map.of(
              "text",
              promptText
                  + (request.imageUrl() != null
                      ? "\\nAnalyze image at URL: " + request.imageUrl()
                      : ""));
      Map<String, Object> content = Map.of("parts", List.of(textPart));
      Map<String, Object> requestPayload = Map.of("contents", List.of(content));

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
              .block(
                  properties.getTimeout() != null
                      ? properties.getTimeout()
                      : Duration.ofSeconds(30));

      String candidateText = extractCandidateText(rawResponse);
      int promptTokens = extractPromptTokens(rawResponse);
      int completionTokens = extractCompletionTokens(rawResponse);
      String finishReason = extractFinishReason(rawResponse);

      return new AIResponse(
          candidateText,
          "v1", // Will be normalized by gateway
          providerName(),
          properties.getModel(),
          promptTokens,
          completionTokens,
          finishReason);

    } catch (WebClientResponseException ex) {
      log.error(
          "Gemini Vision API HTTP error requestId={} status={} body={}",
          requestId,
          ex.getStatusCode(),
          ex.getResponseBodyAsString());
      if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED
          || ex.getStatusCode() == HttpStatus.FORBIDDEN) {
        throw new AIAuthenticationException(
            "Invalid or unauthorized Gemini API key", providerName());
      } else if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
        throw new AIRateLimitException("Gemini API rate limit exceeded", providerName());
      } else if (ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
        throw new AITokenLimitExceededException(
            "Gemini API request payload exceeded token or context limits", providerName());
      } else if (ex.getStatusCode().is5xxServerError()) {
        throw new AIProviderUnavailableException(
            "Gemini API server error: " + ex.getStatusCode(), providerName());
      }
      throw new AIInferenceException(
          "Gemini API returned error status: " + ex.getStatusCode(), providerName());
    } catch (Exception ex) {
      log.error(
          "Gemini Vision API request execution failed requestId={} error={}",
          requestId,
          ex.getMessage());
      if (ex instanceof AIException aie) {
        throw aie;
      }
      if (ex instanceof java.util.concurrent.TimeoutException
          || (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("timeout"))) {
        throw new AITimeoutException("Gemini Vision API request timed out", providerName());
      }
      throw new AIInferenceException(
          "Gemini Vision API inference failed: " + ex.getMessage(), providerName());
    }
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
    // Basic text extraction from Gemini API JSON response candidates structure
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
      log.warn("Failed to extract candidate text, parsing raw response: {}", ex.getMessage());
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
