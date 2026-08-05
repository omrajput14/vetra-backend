package app.vetra.ai.model;

/**
 * Defines the capabilities that an AI provider may support. Used by the ProviderRouter to select
 * the appropriate provider for a given request.
 */
public enum AICapability {

  /** Supports image inputs for multimodal analysis. */
  VISION,

  /** Enforces native JSON schema output from the provider. */
  JSON_MODE,

  /** Supports tool or function calling. */
  FUNCTION_CALLING,

  /** Supports streaming responses via Server-Sent Events (SSE). */
  STREAMING,

  /** Supports extended context windows (greater than 128k tokens). */
  LONG_CONTEXT
}
