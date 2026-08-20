package app.vetra.ai.gateway;

import app.vetra.ai.config.AIGatewayProperties;
import app.vetra.ai.config.ResilienceConfig;
import app.vetra.ai.exception.AIAuthenticationException;
import app.vetra.ai.exception.AIException;
import app.vetra.ai.exception.AIInvalidResponseException;
import app.vetra.ai.exception.AIProviderUnavailableException;
import app.vetra.ai.exception.AIRateLimitException;
import app.vetra.ai.exception.AISafetyViolationException;
import app.vetra.ai.exception.AITimeoutException;
import app.vetra.ai.exception.AITokenLimitExceededException;
import app.vetra.ai.model.AIRequest;
import app.vetra.ai.model.AIResponse;
import app.vetra.ai.prompt.PromptDescriptor;
import app.vetra.ai.provider.AIProvider;
import app.vetra.ai.registry.ProviderRouter;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.tracing.Tracer;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Enterprise Failover Manager.
 *
 * <p>Intercepts AI Gateway execution, managing per-provider Resilience4j Retry, CircuitBreaker, and
 * TimeLimiter instances. Operates transparently to higher business layers.
 */
@Component
public class FailoverManager {

  private static final Logger log = LoggerFactory.getLogger(FailoverManager.class);

  private final ProviderRouter providerRouter;
  private final AIGatewayProperties gatewayProperties;
  private final CircuitBreakerRegistry circuitBreakerRegistry;
  private final RetryRegistry retryRegistry;
  private final MeterRegistry meterRegistry;
  private final Tracer tracer;

  private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
  private final Map<String, Retry> retries = new ConcurrentHashMap<>();

  /**
   * Autowired constructor injection for FailoverManager.
   *
   * @param providerRouter routing component
   * @param gatewayProperties gateway configuration
   * @param circuitBreakerRegistry resilience4j circuit breaker registry
   * @param retryRegistry resilience4j retry registry
   * @param meterRegistry micrometer metrics registry
   * @param tracer opentelemetry tracer (optional)
   */
  @Autowired
  public FailoverManager(
      ProviderRouter providerRouter,
      AIGatewayProperties gatewayProperties,
      CircuitBreakerRegistry circuitBreakerRegistry,
      RetryRegistry retryRegistry,
      MeterRegistry meterRegistry,
      @Autowired(required = false) Tracer tracer) {
    this.providerRouter = providerRouter;
    this.gatewayProperties = gatewayProperties;
    this.circuitBreakerRegistry = circuitBreakerRegistry;
    this.retryRegistry = retryRegistry;
    this.meterRegistry = meterRegistry;
    this.tracer = tracer;
  }

  /**
   * Executes the AI request with per-provider retries, circuit breaking, and transparent failover.
   *
   * @param request the immutable AI request
   * @param renderedPrompt rendered prompt template text
   * @param descriptor prompt descriptor
   * @return normalized AIResponse from the successful provider
   */
  public AIResponse executeWithFailover(
      AIRequest request, String renderedPrompt, PromptDescriptor descriptor) {
    Set<String> excludedProviders = new HashSet<>();
    long startTime = System.nanoTime();
    recordMetric("ai_requests_total", "promptId", request.promptId());

    while (true) {
      ProviderRouter.RoutingDecision decision;
      try {
        decision = providerRouter.route(request, excludedProviders);
      } catch (AIProviderUnavailableException ex) {
        log.error(
            "All providers exhausted for promptId={}. Excluded: {}",
            request.promptId(),
            excludedProviders);
        recordMetric("ai_provider_errors_total", "type", "exhausted");
        throw ex;
      }

      AIProvider provider = decision.provider();
      String providerName = provider.providerName().toLowerCase();

      CircuitBreaker cb = getOrCreateCircuitBreaker(providerName);
      Retry retry = getOrCreateRetry(providerName);

      try {
        log.info(
            "Executing request promptId={} via provider={} model={}",
            request.promptId(),
            provider.providerName(),
            decision.model().alias());

        Supplier<AIResponse> supplier = () -> provider.execute(request, renderedPrompt);
        Supplier<AIResponse> decorated =
            CircuitBreaker.decorateSupplier(cb, Retry.decorateSupplier(retry, supplier));

        AIResponse response = decorated.get();
        recordTimer(
            "ai_latency_seconds",
            System.nanoTime() - startTime,
            "provider",
            provider.providerName());
        return response;

      } catch (AIAuthenticationException
          | AISafetyViolationException
          | AITokenLimitExceededException ex) {
        log.warn(
            "Fast-failing request promptId={} due to unrecoverable exception: {}",
            request.promptId(),
            ex.getClass().getSimpleName());
        recordMetric("ai_provider_errors_total", "provider", provider.providerName());
        throw ex;

      } catch (CallNotPermittedException ex) {
        log.warn("Circuit breaker OPEN for provider={}. Failing over...", provider.providerName());
        recordMetric("ai_circuit_open_total", "provider", provider.providerName());
        recordSpanEvent("circuit_breaker_open", "provider", provider.providerName());
        excludedProviders.add(providerName);
        recordMetric("ai_failover_total", "from", provider.providerName());

      } catch (AIProviderUnavailableException ex) {
        log.warn("Provider unavailable: provider={}. Failing over...", provider.providerName());
        recordMetric("ai_provider_errors_total", "provider", provider.providerName());
        recordSpanEvent("provider_unavailable", "provider", provider.providerName());
        excludedProviders.add(providerName);
        recordMetric("ai_failover_total", "from", provider.providerName());

      } catch (AIException ex) {
        log.warn(
            "Retries exhausted or non-fatal exception for provider={}: {}. Failing over...",
            provider.providerName(),
            ex.getMessage());
        recordMetric("ai_provider_errors_total", "provider", provider.providerName());
        recordSpanEvent("retries_exhausted", "provider", provider.providerName());
        excludedProviders.add(providerName);
        recordMetric("ai_failover_total", "from", provider.providerName());
      }
    }
  }

  /**
   * Retrieves or creates a CircuitBreaker for the specified provider.
   *
   * @param providerName provider identifier
   * @return Resilience4j CircuitBreaker instance
   */
  public CircuitBreaker getOrCreateCircuitBreaker(String providerName) {
    return circuitBreakers.computeIfAbsent(
        providerName,
        name -> {
          AIGatewayProperties.ProviderConfig providerConfig = findProviderConfig(name);
          ResilienceConfig res =
              providerConfig != null ? providerConfig.getResilience() : new ResilienceConfig();

          io.github.resilience4j.circuitbreaker.CircuitBreakerConfig cbConfig =
              io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                  .failureRateThreshold(res.getCircuitBreakerFailureRateThreshold())
                  .waitDurationInOpenState(res.getCircuitBreakerWaitDurationInOpenState())
                  .slidingWindowSize(res.getCircuitBreakerSlidingWindowSize())
                  .recordExceptions(
                      AIProviderUnavailableException.class,
                      AITimeoutException.class,
                      AIRateLimitException.class,
                      AIInvalidResponseException.class,
                      AIException.class)
                  .ignoreExceptions(
                      AIAuthenticationException.class,
                      AISafetyViolationException.class,
                      AITokenLimitExceededException.class)
                  .build();

          CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(name, cbConfig);
          cb.getEventPublisher()
              .onStateTransition(
                  event -> {
                    recordSpanEvent(
                        "circuit_breaker_state_transition",
                        "provider",
                        name,
                        "transition",
                        event.getStateTransition().name());
                    log.info(
                        "CircuitBreaker state transition for provider={}: {}",
                        name,
                        event.getStateTransition());
                  });
          return cb;
        });
  }

  /**
   * Retrieves or creates a Retry instance for the specified provider.
   *
   * @param providerName provider identifier
   * @return Resilience4j Retry instance
   */
  public Retry getOrCreateRetry(String providerName) {
    return retries.computeIfAbsent(
        providerName,
        name -> {
          AIGatewayProperties.ProviderConfig providerConfig = findProviderConfig(name);
          ResilienceConfig res =
              providerConfig != null ? providerConfig.getResilience() : new ResilienceConfig();

          Duration waitDuration = res.getWaitDuration();
          if (waitDuration == null || waitDuration.toMillis() < 1) {
            waitDuration = Duration.ofMillis(10);
          }

          RetryConfig retryConfig =
              RetryConfig.custom()
                  .maxAttempts(res.getMaxRetryAttempts())
                  .intervalFunction(
                      IntervalFunction.ofExponentialBackoff(
                          waitDuration, res.getBackoffMultiplier()))
                  .retryExceptions(
                      AITimeoutException.class,
                      AIRateLimitException.class,
                      AIInvalidResponseException.class)
                  .ignoreExceptions(
                      AIAuthenticationException.class,
                      AISafetyViolationException.class,
                      AITokenLimitExceededException.class,
                      AIProviderUnavailableException.class)
                  .build();

          Retry retry = retryRegistry.retry(name, retryConfig);
          retry
              .getEventPublisher()
              .onRetry(
                  event -> {
                    recordMetric("ai_retry_total", "provider", name);
                    recordSpanEvent(
                        "retry_attempt",
                        "provider",
                        name,
                        "attempt",
                        String.valueOf(event.getNumberOfRetryAttempts()));
                    log.warn(
                        "Retry attempt #{} for provider={}",
                        event.getNumberOfRetryAttempts(),
                        name);
                  });
          return retry;
        });
  }

  private AIGatewayProperties.ProviderConfig findProviderConfig(String providerName) {
    if (gatewayProperties.getProviders() == null) {
      return null;
    }
    return gatewayProperties.getProviders().stream()
        .filter(p -> p.getName() != null && p.getName().equalsIgnoreCase(providerName))
        .findFirst()
        .orElse(null);
  }

  private void recordMetric(String name, String... tags) {
    if (meterRegistry != null) {
      Counter.builder(name).tags(tags).register(meterRegistry).increment();
    }
  }

  private void recordTimer(String name, long durationNanos, String... tags) {
    if (meterRegistry != null) {
      Timer.builder(name)
          .tags(tags)
          .register(meterRegistry)
          .record(durationNanos, TimeUnit.NANOSECONDS);
    }
  }

  private void recordSpanEvent(String eventName, String... keyValues) {
    if (tracer != null && tracer.currentSpan() != null) {
      tracer.currentSpan().event(eventName);
    }
  }
}
