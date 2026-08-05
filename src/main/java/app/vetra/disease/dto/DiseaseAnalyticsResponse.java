package app.vetra.disease.dto;

import app.vetra.disease.entity.DiagnosisConfidenceSource;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive epidemiological analytics response payload.
 *
 * @param totalOutbreaks cumulative total outbreaks count
 * @param activeOutbreaks active outbreaks count
 * @param resolvedOutbreaks resolved outbreaks count
 * @param highRiskOutbreaks high or critical risk outbreaks count
 * @param averageResolutionTimeHours average hours to resolve an outbreak cluster
 * @param diseaseDistribution breakdown of outbreaks per disease
 * @param mostCommonDiseases top most common disease names
 * @param reportsByConfidenceSource breakdown of disease reports by diagnostic confidence source
 */
public record DiseaseAnalyticsResponse(
    long totalOutbreaks,
    long activeOutbreaks,
    long resolvedOutbreaks,
    long highRiskOutbreaks,
    double averageResolutionTimeHours,
    Map<String, Long> diseaseDistribution,
    List<String> mostCommonDiseases,
    Map<DiagnosisConfidenceSource, Long> reportsByConfidenceSource) {}
