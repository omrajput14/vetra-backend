package app.vetra.ai.gateway.governance;

import static org.junit.jupiter.api.Assertions.*;

import app.vetra.ai.config.AIGatewayProperties;
import app.vetra.ai.config.GovernanceProperties;
import app.vetra.ai.exception.AIBudgetExceededException;
import app.vetra.ai.model.AIExecutionContext;
import app.vetra.ai.model.AIRequest;
import app.vetra.ai.model.AIResponse;
import app.vetra.ai.prompt.PromptDescriptor;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AIBudgetManagerTest {

  private AIBudgetManager budgetManager;
  private AIGatewayProperties properties;
  private PromptDescriptor descriptor;

  @BeforeEach
  void setUp() {
    GovernanceProperties governance = new GovernanceProperties();
    governance.getBudget().setTenantDailyTokenLimit(Map.of("tenant-limited", 100L));
    governance.getBudget().setCostPer1kTokens(Map.of("gemini", 0.005));

    properties = AIGatewayProperties.builder().governance(governance).build();
    budgetManager = new AIBudgetManager(properties);

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
  void testCheckBudget_withinLimit_passes() {
    AIRequest request = new AIRequest("test.prompt", Map.of(), null, false, Set.of(), null);
    AIExecutionContext context = AIExecutionContext.of("tenant-limited", "user-1");

    assertDoesNotThrow(() -> budgetManager.checkBudget(request, "Short prompt", descriptor, context));
  }

  @Test
  void testCheckBudget_exceedsLimit_throwsException() {
    AIRequest request = new AIRequest("test.prompt", Map.of(), null, false, Set.of(), null);
    AIExecutionContext context = AIExecutionContext.of("tenant-limited", "user-1");

    // Consume 80 tokens first
    AIResponse firstResponse = new AIResponse("resp", "v1", "gemini", "model", 40, 40, "stop");
    budgetManager.recordConsumption(firstResponse, context);

    // Try to execute request estimated at 50 tokens (80 + 50 = 130 > 100 limit)
    String promptStr = "a".repeat(200); // 200 / 4 = 50 tokens

    assertThrows(
        AIBudgetExceededException.class,
        () -> budgetManager.checkBudget(request, promptStr, descriptor, context));
  }

  @Test
  void testRecordConsumption_calculatesCostAndTracksUsage() {
    AIExecutionContext context = AIExecutionContext.of("tenant-limited", "user-1");
    AIResponse response = new AIResponse("resp", "v1", "gemini", "model", 500, 500, "stop"); // 1000 tokens

    double cost = budgetManager.recordConsumption(response, context);

    assertEquals(0.005, cost, 0.0001);
    assertEquals(1000L, budgetManager.getTenantUsage("tenant-limited"));
  }
}
