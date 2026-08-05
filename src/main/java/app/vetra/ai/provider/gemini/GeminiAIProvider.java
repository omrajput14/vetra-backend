package app.vetra.ai.provider.gemini;

import app.vetra.ai.entity.AIProviderType;
import app.vetra.ai.exception.AIInferenceException;
import app.vetra.ai.exception.AIProviderUnavailableException;
import app.vetra.ai.provider.AIInferenceResult;
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
  private final GeminiPromptBuilder promptBuilder;
  private final GeminiResponseMapper responseMapper;
  private final WebClient webClient;

  /**
   * Constructor injection for GeminiAIProvider.
   *
   * @param properties configuration properties
   * @param promptBuilder prompt engineering component
   * @param responseMapper response mapping component
   * @param webClientBuilder Spring WebClient builder
   */
  public GeminiAIProvider(
      GeminiProperties properties,
      GeminiPromptBuilder promptBuilder,
      GeminiResponseMapper responseMapper,
      WebClient.Builder webClientBuilder) {
    this.properties = properties;
    this.promptBuilder = promptBuilder;
    this.responseMapper = responseMapper;
    this.webClient = webClientBuilder.baseUrl(properties.getBaseUrl()).build();
  }

  @Override
  public boolean supports(AIProviderType type) {
    return type == AIProviderType.GEMINI;
  }

  @Override
  public AIInferenceResult analyze(String imageUrl) {
    validateImageUrl(imageUrl);

    if (!isAvailable()) {
      throw new AIProviderUnavailableException(
          "Gemini Vision provider is disabled or missing API key configuration", "AI_003");
    }

    String requestId = "GEM-" + UUID.randomUUID().toString().substring(0, 8);
    long startTime = System.currentTimeMillis();

    log.info(
        "Dispatching Gemini Vision API inference request requestId={} model={}",
        requestId,
        properties.getModel());

    try {
      Map<String, Object> requestPayload = buildRequestBody(imageUrl);

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

      long latencyMs = System.currentTimeMillis() - startTime;
      log.info(
          "Gemini Vision API request successful requestId={} latencyMs={}", requestId, latencyMs);

      String candidateText = extractCandidateText(rawResponse);
      return responseMapper.mapToInferenceResult(
          candidateText, properties.getModel(), requestId, latencyMs);

    } catch (WebClientResponseException ex) {
      long latencyMs = System.currentTimeMillis() - startTime;
      log.error(
          "Gemini Vision API HTTP error requestId={} status={} body={}",
          requestId,
          ex.getStatusCode(),
          ex.getResponseBodyAsString());

      if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED
          || ex.getStatusCode() == HttpStatus.FORBIDDEN) {
        throw new AIProviderUnavailableException(
            "Invalid or unauthorized Gemini API key", "AI_003");
      } else if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
        throw new AIProviderUnavailableException("Gemini API rate limit exceeded", "AI_003");
      }

      throw new AIInferenceException(
          "Gemini API returned error status: " + ex.getStatusCode(), "AI_004");

    } catch (Exception ex) {
      long latencyMs = System.currentTimeMillis() - startTime;
      log.error(
          "Gemini Vision API request execution failed requestId={} error={}",
          requestId,
          ex.getMessage());

      if (ex instanceof AIProviderUnavailableException aue) {
        throw aue;
      }
      throw new AIInferenceException(
          "Gemini Vision API inference failed: " + ex.getMessage(), "AI_004");
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
  public AIProviderType providerType() {
    return AIProviderType.GEMINI;
  }

  @Override
  public String model() {
    return properties.getModel();
  }

  @Override
  public boolean isAvailable() {
    return properties.isEnabled()
        && properties.getApiKey() != null
        && !properties.getApiKey().isBlank();
  }

  private void validateImageUrl(String imageUrl) {
    if (imageUrl == null || imageUrl.isBlank()) {
      throw new IllegalArgumentException("Image URL must not be null or blank");
    }

    String lower = imageUrl.toLowerCase();
    boolean isValidFormat =
        lower.contains(".jpg")
            || lower.contains(".jpeg")
            || lower.contains(".png")
            || lower.contains("image")
            || lower.startsWith("http://")
            || lower.startsWith("https://");

    if (!isValidFormat) {
      throw new IllegalArgumentException(
          "Unsupported image format or invalid URL. Only JPEG and PNG are supported.");
    }
  }

  private Map<String, Object> buildRequestBody(String imageUrl) {
    String systemPrompt = promptBuilder.buildPrompt();

    Map<String, Object> textPart =
        Map.of("text", systemPrompt + "\nAnalyze image at URL: " + imageUrl);
    Map<String, Object> content = Map.of("parts", List.of(textPart));

    return Map.of("contents", List.of(content));
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
}
