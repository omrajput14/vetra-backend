package app.vetra.ai.entity;

/** Risk classification level evaluated by the AI Veterinary Advisor. */
public enum AIAdvisorRiskLevel {
  CRITICAL,
  SEVERE,
  MODERATE,
  MILD,
  UNKNOWN;

  /**
   * Safely parses a string into an AIAdvisorRiskLevel with UNKNOWN fallback.
   *
   * @param value raw string value
   * @return resolved AIAdvisorRiskLevel
   */
  public static AIAdvisorRiskLevel fromString(String value) {
    if (value == null || value.isBlank()) {
      return UNKNOWN;
    }
    try {
      return AIAdvisorRiskLevel.valueOf(value.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      return UNKNOWN;
    }
  }
}
