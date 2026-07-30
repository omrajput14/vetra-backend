package app.vetra.ai.provider;

/**
 * Strategy interface defining provider contract for AI visual diagnostic inference engines.
 */
public interface AIProvider {

  /**
   * Analyzes a livestock diagnostic image and returns diagnostic recommendations.
   *
   * @param imageUrl public or presigned S3 URL of image
   * @return {@link AIProviderResult} containing diagnosis and confidence score
   */
  AIProviderResult analyzeImage(String imageUrl);

  /**
   * Checks health and availability of the remote or local AI provider service.
   *
   * @return true if provider is active and ready for inference requests, false otherwise
   */
  boolean health();

  /**
   * Returns human-readable name or identifier of the AI provider strategy.
   *
   * @return provider name (e.g. GEMINI, OPENAI, NOOP)
   */
  String providerName();
}
