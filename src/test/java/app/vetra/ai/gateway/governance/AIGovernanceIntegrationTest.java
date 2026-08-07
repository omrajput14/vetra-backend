package app.vetra.ai.gateway.governance;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.vetra.ai.config.AIGatewayProperties;
import app.vetra.ai.config.GovernanceProperties;
import app.vetra.ai.entity.AIProviderType;
import app.vetra.ai.exception.AIBudgetExceededException;
import app.vetra.ai.exception.AIPolicyViolationException;
import app.vetra.ai.exception.AISafetyViolationException;
import app.vetra.ai.gateway.DefaultAIGateway;
import app.vetra.ai.gateway.FailoverManager;
import app.vetra.ai.model.AIExecutionContext;
import app.vetra.ai.model.AIRequest;
import app.vetra.ai.model.AIResponse;
import app.vetra.ai.prompt.PromptDescriptor;
import app.vetra.ai.prompt.PromptRegistry;
import app.vetra.ai.prompt.PromptRenderer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AIGovernanceIntegrationTest {

  private PromptRegistry promptRegistry;
  private PromptRenderer promptRenderer;
  private FailoverManager failoverManager;

  private AISafetyFilter safetyFilter;
  private AIPolicyEngine policyEngine;
  private AIBudgetManager budgetManager;
  private AIAuditService auditService;
  private DefaultAIGovernancePipeline governancePipeline;

  private DefaultAIGateway gateway;
  private AIGatewayProperties properties;

  @BeforeEach
  void setUp() {
    promptRegistry = mock(PromptRegistry.class);
    promptRenderer = mock(PromptRenderer.class);
    failoverManager = mock(FailoverManager.class);

    GovernanceProperties governance = new GovernanceProperties();
    governance.getSafety().setBlockedKeywords(List.of("restricted_keyword"));
    governance.getPolicy().setTenantAllowedProviders(Map.of("tenant-restricted", List.of("gemini")));
    governance.getBudget().setTenantDailyTokenLimit(Map.of("tenant-budget", 50L));

    properties = AIGatewayProperties.builder().governance(governance).build();

    safetyFilter = new AISafetyFilter(properties);
    policyEngine = new AIPolicyEngine(properties);
    budgetManager = new AIBudgetManager(properties);
    auditService = new AIAuditService(properties, null);

    governancePipeline =
        new DefaultAIGovernancePipeline(safetyFilter, policyEngine, budgetManager, auditService);

    org.springframework.cache.CacheManager cacheManager =
        new org.springframework.cache.concurrent.ConcurrentMapCacheManager(app.vetra.infrastructure.cache.CacheNames.AI_DIAGNOSIS);
    app.vetra.ai.cache.AICacheManager aiCacheManager =
        new app.vetra.ai.cache.AICacheManager(cacheManager, properties, null, null);
    app.vetra.ai.cache.CacheKeyGenerator cacheKeyGenerator = new app.vetra.ai.cache.CacheKeyGenerator();

    gateway =
        new DefaultAIGateway(
            promptRegistry,
            promptRenderer,
            failoverManager,
            governancePipeline,
            aiCacheManager,
            cacheKeyGenerator);

    PromptDescriptor descriptor =
        new PromptDescriptor(
            "test.v1",
            "v1",
            "desc",
            "template",
            Set.of(),
            "text",
            0.5,
            0.9,
            100,
            true,
            "STRICT",
            true);

    when(promptRegistry.getPrompt("test.v1")).thenReturn(descriptor);
    when(promptRenderer.render(anyString(), anyMap())).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  void testSafetyViolation_shortCircuitsBeforeFailoverManager() {
    AIRequest request = new AIRequest("test.v1", Map.of(), null, false, Set.of(), null);
    AIExecutionContext context = AIExecutionContext.of("tenant-1", "user-1");

    when(promptRenderer.render(anyString(), anyMap())).thenReturn("Prompt containing restricted_keyword inside");

    assertThrows(AISafetyViolationException.class, () -> gateway.execute(request, context));

    // Verify FailoverManager was NEVER invoked
    verifyNoInteractions(failoverManager);
  }

  @Test
  void testPolicyViolation_shortCircuitsBeforeFailoverManager() {
    AIRequest request =
        new AIRequest("test.v1", Map.of(), null, false, Set.of(), AIProviderType.OPENAI);
    AIExecutionContext context = AIExecutionContext.of("tenant-restricted", "user-1");

    assertThrows(AIPolicyViolationException.class, () -> gateway.execute(request, context));

    // Verify FailoverManager was NEVER invoked
    verifyNoInteractions(failoverManager);
  }

  @Test
  void testBudgetViolation_shortCircuitsBeforeFailoverManager() {
    AIRequest request = new AIRequest("test.v1", Map.of(), null, false, Set.of(), null);
    AIExecutionContext context = AIExecutionContext.of("tenant-budget", "user-1");

    // Consume 40 tokens out of 50 limit
    budgetManager.recordConsumption(
        new AIResponse("resp", "v1", "gemini", "model", 20, 20, "stop"), context);

    when(promptRenderer.render(anyString(), anyMap())).thenReturn("a".repeat(100)); // 100 / 4 = 25 tokens -> 40+25=65 > 50

    assertThrows(AIBudgetExceededException.class, () -> gateway.execute(request, context));

    // Verify FailoverManager was NEVER invoked
    verifyNoInteractions(failoverManager);
  }
}
