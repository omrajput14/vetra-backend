package app.vetra.ai.provider;
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
    String stubJsonResponse =
        """
        {
          "condition": "Healthy",
          "confidence": 0.99,
          "observations": ["No visible abnormalities in the provided image"],
          "recommendations": ["Continue standard care"],
          "requiresVeterinarianReview": false
        }
        """;
    return new AIResponse(
        stubJsonResponse, request.promptId(), PROVIDER_NAME, "noop-v1", 0, 0, "stop");
  }

  /**
   * NoOp supports VISION and JSON_MODE capabilities to allow gateway routing to succeed during
   * tests.
   *
   * @return capability set
   */
  @Override
  public Set<AICapability> supportedCapabilities() {
    return Set.of(AICapability.VISION, AICapability.JSON_MODE);
  }
}
