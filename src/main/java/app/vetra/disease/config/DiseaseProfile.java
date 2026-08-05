package app.vetra.disease.config;

/**
 * Disease-specific epidemiological profile parameters.
 *
 * @param diseaseName disease name identifier
 * @param radiusKm geographic clustering radius in kilometers
 * @param minimumConfirmedCases minimum confirmed cases required to trigger an outbreak cluster
 * @param evaluationWindowHours temporal sliding window in hours
 * @param severityWeight severity multiplier weighting factor (e.g. 1.0 to 3.0)
 * @param reportPriority report priority label (e.g. LOW, MEDIUM, HIGH, URGENT)
 */
public record DiseaseProfile(
    String diseaseName,
    double radiusKm,
    int minimumConfirmedCases,
    int evaluationWindowHours,
    double severityWeight,
    String reportPriority) {

  /**
   * Factory method creating a default profile for unconfigured diseases.
   *
   * @param diseaseName target disease name
   * @return fallback {@link DiseaseProfile}
   */
  public static DiseaseProfile defaultProfile(String diseaseName) {
    return new DiseaseProfile(
        diseaseName,
        15.0, // Default 15km
        3, // Default 3 cases
        72, // Default 72h (3 days)
        1.0, // Standard weight
        "MEDIUM");
  }
}
