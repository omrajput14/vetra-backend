package app.vetra.ai.gateway.governance;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.vetra.ai.exception.AISafetyViolationException;
import app.vetra.ai.model.AIExecutionContext;
import app.vetra.ai.model.AIRequest;
import app.vetra.ai.model.AIResponse;
import app.vetra.ai.prompt.PromptDescriptor;
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
class DefaultAIGovernancePipelineTest {

  @Mock private AISafetyFilter safetyFilter;
  @Mock private AIPolicyEngine policyEngine;
  @Mock private AIBudgetManager budgetManager;
  @Mock private AIAuditService auditService;

  @InjectMocks private DefaultAIGovernancePipeline pipeline;

  private PromptDescriptor descriptor;
  private AIRequest request;
  private AIExecutionContext context;

  @BeforeEach
  void setUp() {
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

    request = new AIRequest("test.prompt", Map.of(), null, false, Set.of(), null);
    context = AIExecutionContext.of("tenant-1", "user-1");
  }

  @Test
  void testExecute_success() {
    AIResponse mockResponse = new AIResponse("resp", "v1", "gemini", "model", 10, 20, "stop");
    Supplier<AIResponse> supplier = () -> mockResponse;

    AIResponse response =
        pipeline.execute(request, "rendered prompt", descriptor, context, supplier);

    assertEquals(mockResponse, response);
    verify(safetyFilter).evaluate(request, "rendered prompt", descriptor, context);
    verify(policyEngine).evaluate(request, "rendered prompt", descriptor, context);
    verify(budgetManager).checkBudget(request, "rendered prompt", descriptor, context);
    verify(budgetManager).recordConsumption(mockResponse, context);
    verify(auditService)
        .recordAuditEvent(
            eq(request),
            eq("rendered prompt"),
            eq(descriptor),
            eq(mockResponse),
            eq(context),
            anyDouble(),
            anyLong(),
            isNull());
  }

  @Test
  void testExecute_safetyViolation_shortCircuitsExecution() {
    doThrow(new AISafetyViolationException("Safety error", "test"))
        .when(safetyFilter)
        .evaluate(request, "rendered prompt", descriptor, context);

    Supplier<AIResponse> supplier = mock(Supplier.class);

    assertThrows(
        AISafetyViolationException.class,
        () -> pipeline.execute(request, "rendered prompt", descriptor, context, supplier));

    verifyNoInteractions(supplier);
  }
}
