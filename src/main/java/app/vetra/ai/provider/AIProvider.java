package app.vetra.ai.provider;

import app.vetra.ai.entity.AIProviderType;

/**
 * Enterprise AI provider contract interface.
 * Providers execute visual diagnostic inference without containing business logic.
 */
public interface AIProvider {

  /**
   * Checks if this provider supports the requested provider type enum.
   *
   * @param type target provider type
   * @return true if supported
   */
  boolean supports(AIProviderType type);

  /**
   * Performs image analysis and returns standard {@link AIInferenceResult}.
   *
   * @param imageUrl public or presigned S3 image URL
   * @return {@link AIInferenceResult} payload
   */
  AIInferenceResult analyze(String imageUrl);

  /**
   * Performs real-time health check against the provider service endpoint.
   *
   * @return true if reachable and healthy
   */
  boolean health();

  /**
   * Returns human-readable provider name.
   *
   * @return name string
   */
  String providerName();

  /**
   * Returns provider enum type.
   *
   * @return {@link AIProviderType}
   */
  AIProviderType providerType();

  /**
   * Returns current active model name or checkpoint identifier.
   *
   * @return model identifier string
   */
  String model();

  /**
   * Returns true if provider is enabled in configuration and passes health checks.
   *
   * @return true if ready for inference
   */
  boolean isAvailable();
}
