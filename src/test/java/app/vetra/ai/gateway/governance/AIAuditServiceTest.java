package app.vetra.ai.gateway.governance;

import static org.junit.jupiter.api.Assertions.*;

import app.vetra.ai.config.AIGatewayProperties;
import app.vetra.ai.config.GovernanceProperties;
import app.vetra.ai.model.AIExecutionContext;
import app.vetra.ai.model.AIRequest;
import app.vetra.ai.model.AIResponse;
import app.vetra.ai.prompt.PromptDescriptor;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AIAuditServiceTest {

  private AIAuditService auditService;
  private AIGatewayProperties properties;
  private PromptDescriptor descriptor;

  @BeforeEach
  void setUp() {
    GovernanceProperties governance = new GovernanceProperties();
    governance.getAudit().setLogPromptContent(false);

    properties = AIGatewayProperties.builder().governance(governance).build();
    auditService = new AIAuditService(properties, null);

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
  void testRecordAuditEvent_success() {
    AIRequest request = new AIRequest("test.prompt", Map.of(), null, false, Set.of(), null);
    AIExecutionContext context = AIExecutionContext.of("tenant-1", "user-1");
    AIResponse response = new AIResponse("resp", "v1", "gemini", "model", 10, 20, "stop");

    assertDoesNotThrow(
        () ->
            auditService.recordAuditEvent(
                request, "rendered prompt", descriptor, response, context, 0.001, 120L, null));
  }

  @Test
  void testRecordAuditEvent_failure() {
    AIRequest request = new AIRequest("test.prompt", Map.of(), null, false, Set.of(), null);
    AIExecutionContext context = AIExecutionContext.of("tenant-1", "user-1");

    assertDoesNotThrow(
        () ->
            auditService.recordAuditEvent(
                request,
                "rendered prompt",
                descriptor,
                null,
                context,
                0.0,
                50L,
                new RuntimeException("Inference failed")));
  }
}
