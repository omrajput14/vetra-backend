package app.vetra.ai.gateway.governance;

import static org.junit.jupiter.api.Assertions.*;

import app.vetra.ai.config.AIGatewayProperties;
import app.vetra.ai.config.GovernanceProperties;
import app.vetra.ai.entity.AIProviderType;
import app.vetra.ai.exception.AIPolicyViolationException;
import app.vetra.ai.model.AIExecutionContext;
import app.vetra.ai.model.AIRequest;
import app.vetra.ai.prompt.PromptDescriptor;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AIPolicyEngineTest {

  private AIPolicyEngine policyEngine;
  private AIGatewayProperties properties;
  private PromptDescriptor descriptor;

  @BeforeEach
  void setUp() {
    GovernanceProperties governance = new GovernanceProperties();
    governance.getPolicy().setTenantAllowedProviders(Map.of("tenant-restricted", List.of("gemini")));
    governance.getPolicy().setMaxPromptTokens(50);

    properties = AIGatewayProperties.builder().governance(governance).build();
    policyEngine = new AIPolicyEngine(properties);

    descriptor =
        new PromptDescriptor(
            "test.prompt",
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
  }

  @Test
  void testEvaluate_allowedProvider_passes() {
    AIRequest request =
        new AIRequest("test.prompt", Map.of(), null, false, Set.of(), AIProviderType.GEMINI);
    AIExecutionContext context = AIExecutionContext.of("tenant-restricted", "user-1");

    assertDoesNotThrow(() -> policyEngine.evaluate(request, "Short prompt", descriptor, context));
  }

  @Test
  void testEvaluate_disallowedProvider_throwsException() {
    AIRequest request =
        new AIRequest("test.prompt", Map.of(), null, false, Set.of(), AIProviderType.OPENAI);
    AIExecutionContext context = AIExecutionContext.of("tenant-restricted", "user-1");

    assertThrows(
        AIPolicyViolationException.class,
        () -> policyEngine.evaluate(request, "Short prompt", descriptor, context));
  }

  @Test
  void testEvaluate_tokenLimitExceeded_throwsException() {
    AIRequest request = new AIRequest("test.prompt", Map.of(), null, false, Set.of(), null);
    AIExecutionContext context = AIExecutionContext.of("tenant-open", "user-1");

    String longPrompt = "a".repeat(300); // 300 / 4 = 75 > 50 max tokens

    assertThrows(
        AIPolicyViolationException.class,
        () -> policyEngine.evaluate(request, longPrompt, descriptor, context));
  }
}
