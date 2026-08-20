package app.vetra.ai.entity;

/** Supported integrated AI diagnostic inference engine providers. */
public enum AIProviderType {
  NONE,
  GEMINI,
  OPENAI,
  CLAUDE,
  OLLAMA,
  NOOP,
  CUSTOM;

  /**
   * Safe parser mapping provider strings to enum constant.
   *
   * @param providerName name of provider
   * @return resolved AIProviderType
   */
  public static AIProviderType fromString(String providerName) {
    if (providerName == null || providerName.isBlank()) {
      return NONE;
    }
    try {
      return AIProviderType.valueOf(providerName.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      return CUSTOM;
    }
  }
}
