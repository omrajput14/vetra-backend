package app.vetra.ai.gateway.governance;

import static org.junit.jupiter.api.Assertions.*;

import app.vetra.ai.config.AIGatewayProperties;
import app.vetra.ai.config.GovernanceProperties;
import app.vetra.ai.exception.AISafetyViolationException;
import app.vetra.ai.model.AIExecutionContext;
import app.vetra.ai.model.AIRequest;
import app.vetra.ai.prompt.PromptDescriptor;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AISafetyFilterTest {

  private AISafetyFilter safetyFilter;
  private AIGatewayProperties properties;
  private PromptDescriptor descriptor;

  @BeforeEach
  void setUp() {
    GovernanceProperties governance = new GovernanceProperties();
    governance.getSafety().setBlockedKeywords(List.of("forbidden_word", "sql_injection"));

    properties = AIGatewayProperties.builder().governance(governance).build();
    safetyFilter = new AISafetyFilter(properties);

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
  void testEvaluate_cleanPrompt_passes() {
    AIRequest request = new AIRequest("test.prompt", Map.of(), null, false, Set.of(), null);
    AIExecutionContext context = AIExecutionContext.of("tenant-1", "user-1");

    assertDoesNotThrow(
        () -> safetyFilter.evaluate(request, "Hello safe world", descriptor, context));
  }

  @Test
  void testEvaluate_blockedKeyword_throwsException() {
    AIRequest request = new AIRequest("test.prompt", Map.of(), null, false, Set.of(), null);
    AIExecutionContext context = AIExecutionContext.of("tenant-1", "user-1");

    assertThrows(
        AISafetyViolationException.class,
        () ->
            safetyFilter.evaluate(
                request, "Hello containing FORBIDDEN_WORD inside", descriptor, context));
  }
}
