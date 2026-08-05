package app.vetra.ai.provider;

import app.vetra.ai.entity.AIProviderType;
import app.vetra.ai.exception.AIProviderUnavailableException;
import app.vetra.ai.model.AICapability;
import app.vetra.ai.model.AIRequest;
import app.vetra.ai.model.AIResponse;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * No-operation AI provider. Used as the default provider during local development, CI environments,
 * and when no external AI provider is configured. Also serves as the last-resort fallback when all
 * real providers are unavailable.
 *
 * <p>This is a first-class provider — it is registered in the {@code ProviderRegistry} and routed
 * to explicitly via the {@code noop} provider name. It does not throw exceptions during routing; it
 * returns a safe, deterministic stub response.
 */
@Component("noOpAIProvider")
public class NoOpAIProvider implements AIProvider {

  private static final String PROVIDER_NAME = "noop";

  @Override
  @Deprecated
  public boolean supports(AIProviderType type) {
    return type == AIProviderType.NONE;
  }

  @Override
  @Deprecated
  public AIInferenceResult analyze(String imageUrl) {
    throw new AIProviderUnavailableException(
        "AI provider is not configured. NoOpAIProvider does not perform inference.", PROVIDER_NAME);
  }

  @Override
  @Deprecated
  public AIProviderType providerType() {
    return AIProviderType.NONE;
  }

  @Override
  @Deprecated
  public String model() {
    return "noop-v1";
  }

  @Override
  public String providerName() {
    return PROVIDER_NAME;
  }

  /**
   * Always returns false. NoOp does not perform real health checks.
   *
   * @return false
   */
  @Override
  public boolean health() {
    return false;
  }

  /**
   * Returns true in local/CI environments (noop is always "available" for routing to succeed).
   * Health is false — the provider is reachable but does not perform real inference.
   *
   * @return true
   */
  @Override
  public boolean isAvailable() {
    return true;
  }

  /**
   * Returns a deterministic stub {@link AIResponse}. Used in CI pipelines and local development to
   * ensure the gateway routing path is fully exercised without external calls.
   *
   * @param request the incoming AI request
   * @param promptText the resolved prompt text (ignored)
   * @return a stub response with zero token usage
   */
  @Override
  public AIResponse execute(AIRequest request, String promptText) {
    return new AIResponse("noop-stub-response", request.promptId(), PROVIDER_NAME, "noop-v1", 0, 0);
  }

  /**
   * NoOp supports no real capabilities. This ensures it is not selected for capability-specific
   * requests unless explicitly configured as the default.
   *
   * @return an empty capability set
   */
  @Override
  public Set<AICapability> supportedCapabilities() {
    return Set.of();
  }
}
