package app.vetra.ai.orchestrator;

import app.vetra.ai.config.AIGatewayProperties;
import app.vetra.ai.entity.AIProviderType;
import app.vetra.ai.entity.AIScan;
import app.vetra.ai.entity.AIScanResultEntity;
import app.vetra.ai.entity.AIScanStatus;
import app.vetra.ai.event.AIInferenceCompletedEvent;
import app.vetra.ai.event.AIInferenceFailedEvent;
import app.vetra.ai.exception.AIInferenceException;
import app.vetra.ai.gateway.AIGateway;
import app.vetra.ai.model.AICapability;
import app.vetra.ai.model.AIExecutionContext;
import app.vetra.ai.model.AIRequest;
import app.vetra.ai.model.AIResponse;
import app.vetra.ai.repository.AIScanRepository;
import app.vetra.ai.repository.AIScanResultRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Enterprise orchestrator coordinating AI provider selection, inference execution, latency
 * measurement, result persistence, metrics tracking, and event publishing via the AI Gateway.
 */
@Service
public class AIOrchestrator {

  private static final Logger log = LoggerFactory.getLogger(AIOrchestrator.class);

  private final AIGateway aiGateway;
  private final AIGatewayProperties properties;
  private final AIScanRepository aiScanRepository;
  private final AIScanResultRepository aiScanResultRepository;
  private final AIMetricsService metricsService;
  private final ApplicationEventPublisher eventPublisher;
  private final ObjectMapper objectMapper;

  /** Constructor injection. */
  public AIOrchestrator(
      AIGateway aiGateway,
      AIGatewayProperties properties,
      AIScanRepository aiScanRepository,
      AIScanResultRepository aiScanResultRepository,
      AIMetricsService metricsService,
      ApplicationEventPublisher eventPublisher) {
    this.aiGateway = aiGateway;
    this.properties = properties;
    this.aiScanRepository = aiScanRepository;
    this.aiScanResultRepository = aiScanResultRepository;
    this.metricsService = metricsService;
    this.eventPublisher = eventPublisher;
    this.objectMapper = new ObjectMapper();
  }

  /** Returns true if AI orchestration platform is enabled in properties. */
  public boolean isAiEnabled() {
    return properties.isEnabled();
  }

  /**
   * Orchestrates inference execution for a scan using the AI Gateway.
   *
   * @param scan target AIScan entity
   * @param requestedProvider optional provider type override
   * @return updated {@link AIScan} entity
   */
  @Transactional
  public AIScan processScan(AIScan scan, AIProviderType requestedProvider) {
    long startTime = System.currentTimeMillis();
    final String targetImageUrl = scan.getImageUrl();

    log.info(
        "AI Orchestrator starting inference for scanId={} user={}",
        scan.getId(),
        scan.getUploadedBy().getId());

    try {
      scan.setStatus(AIScanStatus.PROCESSING);
      AIScan savedScan = aiScanRepository.save(scan);

      AIRequest request = new AIRequest(
          "diagnosis.visual.v1",
          Map.of(),
          targetImageUrl,
          false,
          Set.of(AICapability.VISION, AICapability.JSON_MODE),
          requestedProvider
      );
      
      AIExecutionContext context = AIExecutionContext.of("default", scan.getUploadedBy().getId().toString());

      AIResponse result = aiGateway.execute(request, context);
      long latencyMs = System.currentTimeMillis() - startTime;
      
      DiagnosticParsedResult parsedResult = parseResponseContent(result.content());

      log.info(
          "AI Inference Success scanId={} provider={} model={} latencyMs={}",
          savedScan.getId(),
          result.provider(),
          result.model(),
          latencyMs);

      metricsService.recordSuccess(AIProviderType.valueOf(result.provider().toUpperCase()), latencyMs);
      
      // Update scan with provider details resolved from the gateway
      savedScan.setAiProvider(AIProviderType.valueOf(result.provider().toUpperCase()));
      savedScan.setAiModel(result.model());
      savedScan.setStatus(AIScanStatus.COMPLETED);
      savedScan.setDiagnosis(parsedResult.diagnosis());
      savedScan.setConfidenceScore(parsedResult.confidence());
      savedScan = aiScanRepository.save(savedScan);

      persistInferenceResult(savedScan, result, parsedResult, latencyMs);

      eventPublisher.publishEvent(new AIInferenceCompletedEvent(savedScan.getId()));
      return savedScan;

    } catch (Exception ex) {
      long latencyMs = System.currentTimeMillis() - startTime;
      log.error(
          "AI Inference Failed scanId={} error={}",
          scan.getId(),
          ex.getMessage());
          
      AIProviderType fallbackProvider = requestedProvider != null ? requestedProvider : AIProviderType.NONE;
      metricsService.recordFailure(fallbackProvider, latencyMs);

      scan.setStatus(AIScanStatus.FAILED);
      AIScan savedFailedScan = aiScanRepository.save(scan);

      eventPublisher.publishEvent(
          new AIInferenceFailedEvent(
              savedFailedScan.getId(), ex.getMessage(), fallbackProvider));

      if (ex instanceof RuntimeException rte) {
        throw rte;
      }
      throw new AIInferenceException("AI Orchestrator inference execution failed", "AI_004");
    }
  }

  private void persistInferenceResult(AIScan scan, AIResponse result, DiagnosticParsedResult parsedResult, long latencyMs) {
    AIScanResultEntity resultEntity =
        AIScanResultEntity.builder()
            .scan(scan)
            .provider(AIProviderType.valueOf(result.provider().toUpperCase()))
            .model(result.model())
            .diagnosis(parsedResult.diagnosis())
            .confidence(parsedResult.confidence())
            .rawResponse(result.content())
            .latencyMs(latencyMs)
            .requestId(scan.getId().toString())
            .tokensUsed(result.completionTokens())
            .warnings(parsedResult.warnings() != null ? String.join("; ", parsedResult.warnings()) : null)
            .build();

    aiScanResultRepository.save(resultEntity);
  }
  
  private DiagnosticParsedResult parseResponseContent(String content) {
      try {
          String cleanJson = cleanMarkdownJson(content);
          DiagnosticPayload payload = objectMapper.readValue(cleanJson, DiagnosticPayload.class);
          
          String diagnosis = payload.condition() != null ? payload.condition() : "Unspecified Observation";
          if (payload.observations() != null && !payload.observations().isEmpty()) {
            diagnosis += " | Observations: " + String.join(", ", payload.observations());
          }

          BigDecimal confidence =
              payload.confidence() != null ? payload.confidence() : BigDecimal.valueOf(0.50);

          List<String> warnings = new ArrayList<>();
          if (payload.recommendations() != null && !payload.recommendations().isEmpty()) {
            warnings.add("Recommendations: " + String.join("; ", payload.recommendations()));
          }
          if (Boolean.TRUE.equals(payload.requiresVeterinarianReview())) {
            warnings.add("Requires urgent veterinarian review");
          }
          return new DiagnosticParsedResult(diagnosis, confidence, warnings);
      } catch (Exception e) {
          log.warn("Failed to parse AI Response content, falling back to raw content. Error: {}", e.getMessage());
          return new DiagnosticParsedResult("Unknown Observation", BigDecimal.valueOf(0.10), List.of("Failed to parse diagnostic JSON"));
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
  
  private record DiagnosticPayload(String condition, BigDecimal confidence, List<String> observations, List<String> recommendations, Boolean requiresVeterinarianReview) {}
  private record DiagnosticParsedResult(String diagnosis, BigDecimal confidence, List<String> warnings) {}
}
