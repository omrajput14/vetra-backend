package app.vetra.ai.gateway;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import app.vetra.ai.config.AIGatewayProperties;
import app.vetra.ai.exception.AIAuthenticationException;
import app.vetra.ai.exception.AIProviderUnavailableException;
import app.vetra.ai.exception.AISafetyViolationException;
import app.vetra.ai.exception.AITimeoutException;
import app.vetra.ai.exception.AITokenLimitExceededException;
import app.vetra.ai.model.AICapability;
import app.vetra.ai.model.AIRequest;
import app.vetra.ai.model.AIResponse;
import app.vetra.ai.prompt.PromptDescriptor;
import app.vetra.ai.provider.AIProvider;
import app.vetra.ai.registry.ModelDescriptor;
import app.vetra.ai.registry.ProviderRouter;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FailoverManagerTest {

  @Mock private ProviderRouter providerRouter;
  @Mock private AIProvider geminiProvider;
  @Mock private AIProvider noopProvider;

  private AIGatewayProperties properties;
  private CircuitBreakerRegistry circuitBreakerRegistry;
  private RetryRegistry retryRegistry;
  private SimpleMeterRegistry meterRegistry;
  private FailoverManager failoverManager;

  private PromptDescriptor mockDescriptor;
  private AIRequest request;
  private ModelDescriptor geminiModel;
  private ModelDescriptor noopModel;

  @BeforeEach
  void setUp() {
    properties = AIGatewayProperties.builder().build();
    circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
    retryRegistry = RetryRegistry.ofDefaults();
    meterRegistry = new SimpleMeterRegistry();

    failoverManager =
        new FailoverManager(
            providerRouter, properties, circuitBreakerRegistry, retryRegistry, meterRegistry, null);

    mockDescriptor =
        new PromptDescriptor(
            "test.v1",
            "v1",
            "desc",
            "template",
            Set.of(AICapability.VISION),
            "json",
            0.5,
            0.9,
            1000,
            true);

    request = new AIRequest("test.v1", Map.of(), null, false, Set.of(), null);
    geminiModel =
        new ModelDescriptor("gemini-flash", "gemini-alias", "GEMINI", Set.of(), 100, 100, true);
    noopModel = new ModelDescriptor("noop-v1", "noop-alias", "noop", Set.of(), 100, 100, true);
  }

  @Test
  void testExecute_successFirstAttempt() {
    when(geminiProvider.providerName()).thenReturn("GEMINI");
    when(providerRouter.route(eq(request), anySet()))
        .thenReturn(new ProviderRouter.RoutingDecision(geminiProvider, geminiModel));

    AIResponse expectedResponse =
        new AIResponse("success", "v1", "GEMINI", "gemini-flash", 10, 10, "STOP");
    when(geminiProvider.execute(eq(request), eq("rendered"))).thenReturn(expectedResponse);

    AIResponse actual = failoverManager.executeWithFailover(request, "rendered", mockDescriptor);

    assertEquals("success", actual.content());
    assertEquals("GEMINI", actual.provider());
  }

  @Test
  void testExecute_retrySuccess() {
    when(geminiProvider.providerName()).thenReturn("GEMINI");
    when(providerRouter.route(eq(request), anySet()))
        .thenReturn(new ProviderRouter.RoutingDecision(geminiProvider, geminiModel));

    AIResponse expectedResponse =
        new AIResponse("success after retry", "v1", "GEMINI", "gemini-flash", 10, 10, "STOP");

    // First attempt fails with timeout, second succeeds
    when(geminiProvider.execute(eq(request), eq("rendered")))
        .thenThrow(new AITimeoutException("timeout", "GEMINI"))
        .thenReturn(expectedResponse);

    AIResponse actual = failoverManager.executeWithFailover(request, "rendered", mockDescriptor);

    assertEquals("success after retry", actual.content());
    verify(geminiProvider, times(2)).execute(eq(request), eq("rendered"));
  }

  @Test
  void testExecute_retryExhaustion_failoverToNoOp() {
    when(geminiProvider.providerName()).thenReturn("GEMINI");
    when(noopProvider.providerName()).thenReturn("noop");

    // First route to GEMINI, after GEMINI fails, route to noop
    when(providerRouter.route(eq(request), anySet()))
        .thenReturn(new ProviderRouter.RoutingDecision(geminiProvider, geminiModel))
        .thenReturn(new ProviderRouter.RoutingDecision(noopProvider, noopModel));

    // Gemini repeatedly times out until retries exhausted
    when(geminiProvider.execute(eq(request), eq("rendered")))
        .thenThrow(new AITimeoutException("timeout", "GEMINI"));

    AIResponse noopResponse = new AIResponse("noop output", "v1", "noop", "noop-v1", 0, 0, "STOP");
    when(noopProvider.execute(eq(request), eq("rendered"))).thenReturn(noopResponse);

    AIResponse actual = failoverManager.executeWithFailover(request, "rendered", mockDescriptor);

    assertEquals("noop output", actual.content());
    assertEquals("noop", actual.provider());
    verify(geminiProvider, atLeast(2)).execute(eq(request), eq("rendered"));
    verify(noopProvider, times(1)).execute(eq(request), eq("rendered"));
  }

  @Test
  void testExecute_fastFail_authenticationException() {
    when(geminiProvider.providerName()).thenReturn("GEMINI");
    when(providerRouter.route(eq(request), anySet()))
        .thenReturn(new ProviderRouter.RoutingDecision(geminiProvider, geminiModel));

    when(geminiProvider.execute(eq(request), eq("rendered")))
        .thenThrow(new AIAuthenticationException("invalid key", "GEMINI"));

    assertThrows(
        AIAuthenticationException.class,
        () -> failoverManager.executeWithFailover(request, "rendered", mockDescriptor));

    verify(geminiProvider, times(1)).execute(eq(request), eq("rendered"));
    verify(providerRouter, times(1)).route(eq(request), anySet());
  }

  @Test
  void testExecute_fastFail_safetyViolationException() {
    when(geminiProvider.providerName()).thenReturn("GEMINI");
    when(providerRouter.route(eq(request), anySet()))
        .thenReturn(new ProviderRouter.RoutingDecision(geminiProvider, geminiModel));

    when(geminiProvider.execute(eq(request), eq("rendered")))
        .thenThrow(new AISafetyViolationException("unsafe content", "GEMINI"));

    assertThrows(
        AISafetyViolationException.class,
        () -> failoverManager.executeWithFailover(request, "rendered", mockDescriptor));
  }

  @Test
  void testExecute_fastFail_tokenLimitExceededException() {
    when(geminiProvider.providerName()).thenReturn("GEMINI");
    when(providerRouter.route(eq(request), anySet()))
        .thenReturn(new ProviderRouter.RoutingDecision(geminiProvider, geminiModel));

    when(geminiProvider.execute(eq(request), eq("rendered")))
        .thenThrow(new AITokenLimitExceededException("too long", "GEMINI"));

    assertThrows(
        AITokenLimitExceededException.class,
        () -> failoverManager.executeWithFailover(request, "rendered", mockDescriptor));
  }

  @Test
  void testExecute_immediateFailover_providerUnavailable() {
    when(geminiProvider.providerName()).thenReturn("GEMINI");
    when(noopProvider.providerName()).thenReturn("noop");

    when(providerRouter.route(eq(request), anySet()))
        .thenReturn(new ProviderRouter.RoutingDecision(geminiProvider, geminiModel))
        .thenReturn(new ProviderRouter.RoutingDecision(noopProvider, noopModel));

    when(geminiProvider.execute(eq(request), eq("rendered")))
        .thenThrow(new AIProviderUnavailableException("service down", "GEMINI"));

    AIResponse noopResponse = new AIResponse("noop output", "v1", "noop", "noop-v1", 0, 0, "STOP");
    when(noopProvider.execute(eq(request), eq("rendered"))).thenReturn(noopResponse);

    AIResponse actual = failoverManager.executeWithFailover(request, "rendered", mockDescriptor);

    assertEquals("noop output", actual.content());
    // Should NOT retry gemini, immediately switch to noop
    verify(geminiProvider, times(1)).execute(eq(request), eq("rendered"));
    verify(noopProvider, times(1)).execute(eq(request), eq("rendered"));
  }

  @Test
  void testExecute_circuitBreakerOpen_immediateFailover() {
    when(geminiProvider.providerName()).thenReturn("gemini");
    when(noopProvider.providerName()).thenReturn("noop");

    CircuitBreaker cb = failoverManager.getOrCreateCircuitBreaker("gemini");
    cb.transitionToOpenState();

    when(providerRouter.route(eq(request), anySet()))
        .thenReturn(new ProviderRouter.RoutingDecision(geminiProvider, geminiModel))
        .thenReturn(new ProviderRouter.RoutingDecision(noopProvider, noopModel));

    AIResponse noopResponse = new AIResponse("noop output", "v1", "noop", "noop-v1", 0, 0, "STOP");
    when(noopProvider.execute(eq(request), eq("rendered"))).thenReturn(noopResponse);

    AIResponse actual = failoverManager.executeWithFailover(request, "rendered", mockDescriptor);

    assertEquals("noop output", actual.content());
    // Gemini execute should never be called when circuit breaker is OPEN
    verify(geminiProvider, never()).execute(eq(request), eq("rendered"));
    verify(noopProvider, times(1)).execute(eq(request), eq("rendered"));
  }
}
