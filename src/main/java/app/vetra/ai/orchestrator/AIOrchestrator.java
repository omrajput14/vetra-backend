package app.vetra.ai.orchestrator;

import app.vetra.ai.entity.AIProviderType;
import app.vetra.ai.entity.AIScan;
import app.vetra.ai.entity.AIScanResultEntity;
import app.vetra.ai.entity.AIScanStatus;
import app.vetra.ai.event.AIInferenceCompletedEvent;
import app.vetra.ai.event.AIInferenceFailedEvent;
import app.vetra.ai.exception.AIInferenceException;
import app.vetra.ai.provider.AIInferenceResult;
import app.vetra.ai.provider.AIProvider;
import app.vetra.ai.repository.AIScanRepository;
import app.vetra.ai.repository.AIScanResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Enterprise orchestrator coordinating AI provider selection, inference execution, latency
 * measurement, result persistence, metrics tracking, and event publishing.
 */
@Service
public class AIOrchestrator {

  private static final Logger log = LoggerFactory.getLogger(AIOrchestrator.class);

  private final AIProviderRegistry providerRegistry;
  private final AIRetryPolicy retryPolicy;
  private final AIScanRepository aiScanRepository;
  private final AIScanResultRepository aiScanResultRepository;
  private final AIMetricsService metricsService;
  private final ApplicationEventPublisher eventPublisher;

  /** Constructor injection. */
  public AIOrchestrator(
      AIProviderRegistry providerRegistry,
      AIRetryPolicy retryPolicy,
      AIScanRepository aiScanRepository,
      AIScanResultRepository aiScanResultRepository,
      AIMetricsService metricsService,
      ApplicationEventPublisher eventPublisher) {
    this.providerRegistry = providerRegistry;
    this.retryPolicy = retryPolicy;
    this.aiScanRepository = aiScanRepository;
    this.aiScanResultRepository = aiScanResultRepository;
    this.metricsService = metricsService;
    this.eventPublisher = eventPublisher;
  }

  /** Returns true if AI orchestration platform is enabled in properties. */
  public boolean isAiEnabled() {
    return providerRegistry.isPlatformEnabled();
  }

  /**
   * Orchestrates inference execution for a scan using requested or default AI provider.
   *
   * @param scan target AIScan entity
   * @param requestedProvider optional provider type override
   * @return updated {@link AIScan} entity
   */
  @Transactional
  public AIScan processScan(AIScan scan, AIProviderType requestedProvider) {
    AIProvider provider = providerRegistry.getProvider(requestedProvider);
    long startTime = System.currentTimeMillis();
    final String targetImageUrl = scan.getImageUrl();

    log.info(
        "AI Orchestrator starting inference for scanId={} user={}",
        scan.getId(),
        scan.getUploadedBy().getId());

    try {
      scan.setStatus(AIScanStatus.PROCESSING);
      scan.setAiProvider(provider.providerType());
      scan.setAiModel(provider.model());
      AIScan savedScan = aiScanRepository.save(scan);

      AIInferenceResult result =
          retryPolicy.executeWithRetry(() -> provider.analyze(targetImageUrl));
      long latencyMs = System.currentTimeMillis() - startTime;

      // Audit log without image contents
      log.info(
          "AI Inference Success scanId={} provider={} model={} latencyMs={}",
          savedScan.getId(),
          result.provider(),
          result.model(),
          latencyMs);

      metricsService.recordSuccess(result.provider(), latencyMs);
      persistInferenceResult(savedScan, result, latencyMs);

      savedScan.setStatus(AIScanStatus.COMPLETED);
      savedScan.setDiagnosis(result.diagnosis());
      savedScan.setConfidenceScore(result.confidence());
      savedScan = aiScanRepository.save(savedScan);

      eventPublisher.publishEvent(new AIInferenceCompletedEvent(savedScan.getId(), result));
      return savedScan;

    } catch (Exception ex) {
      long latencyMs = System.currentTimeMillis() - startTime;
      log.error(
          "AI Inference Failed scanId={} provider={} error={}",
          scan.getId(),
          provider.providerType(),
          ex.getMessage());

      metricsService.recordFailure(provider.providerType(), latencyMs);

      scan.setStatus(AIScanStatus.FAILED);
      AIScan savedFailedScan = aiScanRepository.save(scan);

      eventPublisher.publishEvent(
          new AIInferenceFailedEvent(
              savedFailedScan.getId(), ex.getMessage(), provider.providerType()));

      if (ex instanceof RuntimeException rte) {
        throw rte;
      }
      throw new AIInferenceException("AI Orchestrator inference execution failed", "AI_004");
    }
  }

  private void persistInferenceResult(AIScan scan, AIInferenceResult result, long latencyMs) {
    AIScanResultEntity resultEntity =
        AIScanResultEntity.builder()
            .scan(scan)
            .provider(result.provider())
            .model(result.model())
            .diagnosis(result.diagnosis())
            .confidence(result.confidence())
            .rawResponse(result.rawResponse())
            .latencyMs(latencyMs)
            .requestId(result.requestId())
            .tokensUsed(result.tokensUsed())
            .warnings(result.warnings() != null ? String.join("; ", result.warnings()) : null)
            .build();

    aiScanResultRepository.save(resultEntity);
  }
}
