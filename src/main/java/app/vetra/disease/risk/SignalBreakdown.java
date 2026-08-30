package app.vetra.disease.risk;

/**
 * Structured breakdown of the contributing epidemiological signals in the risk assessment.
 */
public record SignalBreakdown(
    double clusterRawScore,
    double clusterWeightedPoints,
    double weatherRawScore,
    double weatherWeightedPoints,
    double historyRawScore,
    double historyWeightedPoints,
    double vaccinationGapRawScore,
    double vaccinationGapWeightedPoints,
    Double weatherTemperature,
    Double weatherHumidity,
    Double weatherPrecipitation,
    boolean weatherAvailable,
    Double vaccinationCoveragePct,
    int totalAnimalsInRadius,
    int vaccinatedAnimalsInRadius,
    int historicalRecurrenceCount,
    boolean seasonalEpidemicPeak,
    String explanation,
    String recommendedAction) {}
