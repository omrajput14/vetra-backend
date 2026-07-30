package app.vetra.ai.provider;

import org.springframework.stereotype.Component;

/**
 * Fallback no-op AI provider implementation used before external AI models are integrated.
 */
@Component("noOpAIProvider")
public class NoOpAIProvider implements AIProvider {

  @Override
  public AIProviderResult analyzeImage(String imageUrl) {
    throw new UnsupportedOperationException("AI provider not configured.");
  }

  @Override
  public boolean health() {
    return false;
  }

  @Override
  public String providerName() {
    return "NOOP";
  }
}
