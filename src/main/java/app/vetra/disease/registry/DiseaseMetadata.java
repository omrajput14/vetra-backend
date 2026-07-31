package app.vetra.disease.registry;

/**
 * Metadata descriptor for livestock diseases in the Disease Registry.
 *
 * @param diseaseName disease identifier
 * @param severity baseline severity level (LOW, MEDIUM, HIGH, CRITICAL)
 * @param zoonotic whether disease is transmissible to humans
 * @param reportable whether disease is legally reportable to government health authorities
 * @param mortality baseline mortality risk (LOW, MEDIUM, HIGH, VERY_HIGH)
 * @param defaultRadiusKm default outbreak surveillance radius in kilometers
 * @param minimumCases minimum cases required to trigger outbreak cluster
 * @param evaluationWindowHours temporal sliding window in hours
 */
public record DiseaseMetadata(
    String diseaseName,
    String severity,
    boolean zoonotic,
    boolean reportable,
    String mortality,
    double defaultRadiusKm,
    int minimumCases,
    int evaluationWindowHours
) {}
