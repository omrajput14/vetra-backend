package app.vetra.ai.provider;

import app.vetra.ai.entity.AIProviderType;
import app.vetra.ai.exception.AIProviderUnavailableException;
import org.springframework.stereotype.Component;

/**
 * Fallback NoOp AI provider implementation used when no active external provider is enabled.
 */
@Component("noOpAIProvider")
public class NoOpAIProvider implements AIProvider {

  @Override
  public boolean supports(AIProviderType type) {
    return type == AIProviderType.NONE;
  }

  @Override
  public AIInferenceResult analyze(String imageUrl) {
    throw new AIProviderUnavailableException("AI provider is not configured or disabled.");
  }

  @Override
  public boolean health() {
    return false;
  }

  @Override
  public String providerName() {
    return "NOOP";
  }

  @Override
  public AIProviderType providerType() {
    return AIProviderType.NONE;
  }

  @Override
  public String model() {
    return "noop-v1";
  }

  @Override
  public boolean isAvailable() {
    return false;
  }
}
