package app.vetra.ai.gateway;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import app.vetra.ai.exception.AIConfigurationException;
import app.vetra.ai.gateway.governance.AIGovernancePipeline;
import app.vetra.ai.model.AICapability;
import app.vetra.ai.model.AIExecutionContext;
import app.vetra.ai.model.AIRequest;
import app.vetra.ai.model.AIResponse;
import app.vetra.ai.prompt.PromptDescriptor;
import app.vetra.ai.prompt.PromptRegistry;
import app.vetra.ai.prompt.PromptRenderer;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultAIGatewayTest {

  @Mock private PromptRegistry promptRegistry;
  @Mock private PromptRenderer promptRenderer;
  @Mock private FailoverManager failoverManager;
  @Mock private AIGovernancePipeline governancePipeline;
  @Mock private app.vetra.ai.cache.AICacheManager aiCacheManager;
  @Mock private app.vetra.ai.cache.CacheKeyGenerator cacheKeyGenerator;

  @InjectMocks private DefaultAIGateway aiGateway;

  private PromptDescriptor mockDescriptor;

  @BeforeEach
  void setUp() {
    mockDescriptor =
        new PromptDescriptor(
            "test.v1",
            "v1",
            "desc",
            "Template {{var}}",
            Set.of(AICapability.VISION),
            "json",
            0.5,
            0.9,
            1000,
            true);
  }

  @Test
  void testExecute_success() {
    AIRequest request = new AIRequest("test.v1", Map.of("var", "val"), null, false, Set.of(), null);
    AIExecutionContext context = AIExecutionContext.of("tenant-1", "user-1");

    when(promptRegistry.getPrompt("test.v1")).thenReturn(mockDescriptor);
    when(promptRenderer.render(mockDescriptor.template(), request.variables()))
        .thenReturn("Template val");
    when(cacheKeyGenerator.generateKey(any(), any(), any())).thenReturn("test-key");
    when(aiCacheManager.getOrCompute(eq("test-key"), eq("v1"), eq(false), any()))
        .thenReturn(new AIResponse("content", "test.v1", "noop-provider", "noop-model", 10, 20, "stop"));

    AIResponse mockResponse =
        new AIResponse("content", "test.v1", "noop-provider", "noop-model", 10, 20, "stop");

    when(governancePipeline.execute(
            any(AIRequest.class),
            eq("Template val"),
            eq(mockDescriptor),
            eq(context),
            any()))
        .thenAnswer(
            invocation -> {
              Supplier<AIResponse> supplier = invocation.getArgument(4);
              return supplier.get();
            });

    AIResponse response = aiGateway.execute(request, context);

    assertEquals("content", response.content());
    assertEquals("test.v1", response.promptVersion());
    assertEquals("noop-provider", response.provider());
    assertEquals("noop-model", response.model());
    assertEquals(10, response.promptTokens());
    assertEquals(20, response.completionTokens());
    assertEquals(30, response.totalTokens());
  }

  @Test
  void testExecute_missingPromptId() {
    AIRequest request = new AIRequest(null, Map.of(), null, false, Set.of(), null);

    assertThrows(AIConfigurationException.class, () -> aiGateway.execute(request));
  }
}
