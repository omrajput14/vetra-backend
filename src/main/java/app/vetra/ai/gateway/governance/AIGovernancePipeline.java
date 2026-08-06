package app.vetra.ai.gateway.governance;

import app.vetra.ai.model.AIExecutionContext;
import app.vetra.ai.model.AIRequest;
import app.vetra.ai.model.AIResponse;
import app.vetra.ai.prompt.PromptDescriptor;
import java.util.function.Supplier;

/**
 * Pipeline contract for evaluating AI governance controls (safety, policy, budget, auditing)
 * around request execution.
 */
public interface AIGovernancePipeline {

  /**
   * Executes governance checks and audits around the AI execution chain.
   *
   * @param request AI request
   * @param renderedPrompt rendered prompt template
   * @param descriptor prompt descriptor
   * @param context execution context
   * @param executionChain supplier executing downstream resilience & provider call
   * @return normalized AI response
   */
  AIResponse execute(
      AIRequest request,
      String renderedPrompt,
      PromptDescriptor descriptor,
      AIExecutionContext context,
      Supplier<AIResponse> executionChain);
}
