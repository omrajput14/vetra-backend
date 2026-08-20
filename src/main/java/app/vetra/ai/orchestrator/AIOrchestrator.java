package app.vetra.ai.orchestrator;

import app.vetra.ai.agent.gateway.AgentGateway;
import app.vetra.ai.agent.model.AgentCapability;
import app.vetra.ai.agent.model.AgentRequest;
import app.vetra.ai.agent.model.AgentResponse;
import app.vetra.ai.config.AIGatewayProperties;
import app.vetra.ai.entity.AIProviderType;
import app.vetra.ai.entity.AIScan;
import app.vetra.ai.entity.AIScanResultEntity;
import app.vetra.ai.entity.AIScanStatus;
import app.vetra.ai.event.AIInferenceCompletedEvent;
import app.vetra.ai.event.AIInferenceFailedEvent;
import app.vetra.ai.exception.AIInferenceException;
import app.vetra.ai.model.AIExecutionContext;
import app.vetra.ai.model.AIResponse;
import app.vetra.ai.repository.AIScanRepository;
import app.vetra.ai.repository.AIScanResultRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Enterprise orchestrator coordinating AI provider selection, inference execution, latency
 * measurement, result persistence, metrics tracking, and event publishing via the Agent Gateway.
 */
@Service
public class AIOrchestrator {

  private static final Logger log = LoggerFactory.getLogger(AIOrchestrator.class);

  private final AgentGateway agentGateway;
  private final AIGatewayProperties properties;
  private final AIScanRepository aiScanRepository;
  private final AIScanResultRepository aiScanResultRepository;
  private final AIMetricsService metricsService;
  private final ApplicationEventPublisher eventPublisher;
  private final ObjectMapper objectMapper;

  /** Constructor injection. */
  public AIOrchestrator(
      AgentGateway agentGateway,
      AIGatewayProperties properties,
      AIScanRepository aiScanRepository,
      AIScanResultRepository aiScanResultRepository,
      AIMetricsService metricsService,
      ApplicationEventPublisher eventPublisher) {
    this.agentGateway = agentGateway;
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
   * Orchestrates inference execution for a scan using the Agent Gateway.
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

      AIExecutionContext context =
          AIExecutionContext.of("default", scan.getUploadedBy().getId().toString());

      AgentRequest agentRequest =
          AgentRequest.ofVision(AgentCapability.DIAGNOSIS, targetImageUrl, context);

      AgentResponse agentResponse = agentGateway.execute(agentRequest);
      AIResponse result = agentResponse.rawResponse();
      long latencyMs = System.currentTimeMillis() - startTime;

      DiagnosticParsedResult parsedResult = parseResponseContent(result.content());

      log.info(
          "AI Inference Success scanId={} agent={} provider={} model={} latencyMs={}",
          savedScan.getId(),
          agentResponse.agentName(),
          result.provider(),
          result.model(),
          latencyMs);

      AIProviderType resolvedProvider = AIProviderType.fromString(result.provider());
      metricsService.recordSuccess(resolvedProvider, latencyMs);

      savedScan.setAiProvider(resolvedProvider);
      savedScan.setAiModel(result.model());
      savedScan.setStatus(AIScanStatus.COMPLETED);
      savedScan.setDiagnosis(parsedResult.diagnosis());
      savedScan.setConfidenceScore(parsedResult.confidence());
      savedScan.setNotes(parsedResult.notesJson());
      savedScan = aiScanRepository.save(savedScan);

      persistInferenceResult(savedScan, result, parsedResult, latencyMs);

      eventPublisher.publishEvent(new AIInferenceCompletedEvent(savedScan.getId()));
      return savedScan;

    } catch (Exception ex) {
      long latencyMs = System.currentTimeMillis() - startTime;
      log.error("AI Inference Failed scanId={} error={}", scan.getId(), ex.getMessage());

      AIProviderType fallbackProvider =
          requestedProvider != null ? requestedProvider : AIProviderType.NONE;
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

  private void persistInferenceResult(
      AIScan scan,
      AIResponse result,
      DiagnosticParsedResult parsedResult,
      long latencyMs) {
    AIScanResultEntity resultEntity =
        AIScanResultEntity.builder()
            .scan(scan)
            .provider(AIProviderType.fromString(result.provider()))
            .model(result.model())
            .diagnosis(parsedResult.diagnosis())
            .confidence(parsedResult.confidence())
            .rawResponse(result.content())
            .latencyMs(latencyMs)
            .requestId(scan.getId().toString())
            .tokensUsed(result.completionTokens())
            .warnings(
                parsedResult.warnings() != null
                    ? String.join("; ", parsedResult.warnings())
                    : null)
            .build();

    aiScanResultRepository.save(resultEntity);
  }

  private DiagnosticParsedResult parseResponseContent(String content) {
    try {
      String cleanJson = cleanMarkdownJson(content);
      DiagnosticPayload payload = objectMapper.readValue(cleanJson, DiagnosticPayload.class);

      String diagnosis = payload.resolvedCondition();
      BigDecimal confidence = payload.resolvedConfidence();

      List<String> warnings = new ArrayList<>();
      if (Boolean.TRUE.equals(payload.requiresVeterinarianReview())) {
        warnings.add("Requires veterinarian clinical review");
      }

      StructuredNotesPayload notesPayload =
          new StructuredNotesPayload(
              payload.resolvedSeverity(),
              payload.observations() != null ? payload.observations() : List.of(),
              payload.resolvedRecommendedNextStep(),
              payload.requiresVeterinarianReview() != null
                  ? payload.requiresVeterinarianReview()
                  : true,
              payload.resolvedDisclaimer());

      String notesJson = objectMapper.writeValueAsString(notesPayload);
      return new DiagnosticParsedResult(diagnosis, confidence, notesJson, warnings);

    } catch (Exception e) {
      log.warn("Failed to parse diagnostic JSON, falling back. Error: {}", e.getMessage());
      StructuredNotesPayload fallbackNotes =
          new StructuredNotesPayload(
              "UNKNOWN",
              List.of("Automated feature extraction incomplete"),
              "Veterinary clinical evaluation recommended for visual assessment.",
              true,
              "This is an AI-assisted preliminary assessment and is not a confirmed diagnosis.");

      String notesJson;
      try {
        notesJson = objectMapper.writeValueAsString(fallbackNotes);
      } catch (Exception ignored) {
        notesJson = "{}";
      }
      return new DiagnosticParsedResult(
          "Inconclusive / Insufficient Visual Evidence",
          BigDecimal.valueOf(0.20),
          notesJson,
          List.of("Failed to parse diagnostic JSON"));
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

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record DiagnosticPayload(
      String possibleCondition,
      String condition,
      BigDecimal confidence,
      String severity,
      List<String> observations,
      String recommendedNextStep,
      List<String> recommendations,
      Boolean requiresVeterinarianReview,
      String disclaimer) {

    public String resolvedCondition() {
      if (possibleCondition != null && !possibleCondition.isBlank()) {
        return possibleCondition.trim();
      }
      if (condition != null && !condition.isBlank()) {
        return condition.trim();
      }
      return "Inconclusive / Insufficient Visual Evidence";
    }

    public BigDecimal resolvedConfidence() {
      if (confidence != null) {
        return confidence.min(BigDecimal.ONE).max(BigDecimal.ZERO);
      }
      return BigDecimal.valueOf(0.50);
    }

    public String resolvedSeverity() {
      if (severity != null && !severity.isBlank()) {
        return severity.trim().toUpperCase();
      }
      return "UNKNOWN";
    }

    public String resolvedRecommendedNextStep() {
      if (recommendedNextStep != null && !recommendedNextStep.isBlank()) {
        return recommendedNextStep.trim();
      }
      if (recommendations != null && !recommendations.isEmpty()) {
        return String.join("; ", recommendations);
      }
      return "Schedule a clinical examination with a licensed veterinarian for evaluation.";
    }

    public String resolvedDisclaimer() {
      if (disclaimer != null && !disclaimer.isBlank()) {
        return disclaimer.trim();
      }
      return "This is an AI-assisted preliminary assessment and is not a confirmed veterinary diagnosis.";
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record StructuredNotesPayload(
      String severity,
      List<String> observations,
      String recommendedNextStep,
      Boolean requiresVeterinarianReview,
      String disclaimer) {}

  private record DiagnosticParsedResult(
      String diagnosis,
      BigDecimal confidence,
      String notesJson,
      List<String> warnings) {}
}
