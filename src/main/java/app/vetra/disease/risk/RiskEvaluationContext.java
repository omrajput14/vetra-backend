package app.vetra.disease.risk;

/**
 * Parameter encapsulation for multi-signal livestock health risk evaluations.
 *
 * @param diseaseName target disease/pathogen
 * @param confirmedCases count of veterinary or lab-confirmed disease reports
 * @param suspectedCases count of unverified field or farmer reports
 * @param vetConfirmedMortalities count of veterinarian-confirmed mortality events
 * @param farmerReportedMortalities count of farmer-reported mortality events
 * @param latitude cluster center latitude
 * @param longitude cluster center longitude
 * @param radiusKm cluster radius in kilometers
 * @param windowHours evaluation sliding temporal window in hours
 */
public record RiskEvaluationContext(
    String diseaseName,
    int confirmedCases,
    int suspectedCases,
    int vetConfirmedMortalities,
    int farmerReportedMortalities,
    double latitude,
    double longitude,
    double radiusKm,
    int windowHours) {}
