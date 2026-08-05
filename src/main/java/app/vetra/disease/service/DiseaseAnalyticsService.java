package app.vetra.disease.service;

import app.vetra.disease.dto.DiseaseAnalyticsResponse;
import app.vetra.disease.entity.DiagnosisConfidenceSource;
import app.vetra.disease.entity.DiseaseReport;
import app.vetra.disease.entity.Outbreak;
import app.vetra.disease.entity.OutbreakRiskScore;
import app.vetra.disease.entity.OutbreakStatus;
import app.vetra.disease.repository.DiseaseReportRepository;
import app.vetra.disease.repository.OutbreakRepository;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service generating high-level epidemiological analytics and disease surveillance metrics. */
@Service
public class DiseaseAnalyticsService {

  private final OutbreakRepository outbreakRepository;
  private final DiseaseReportRepository diseaseReportRepository;

  /** Constructor injection. */
  public DiseaseAnalyticsService(
      OutbreakRepository outbreakRepository, DiseaseReportRepository diseaseReportRepository) {
    this.outbreakRepository = outbreakRepository;
    this.diseaseReportRepository = diseaseReportRepository;
  }

  /**
   * Calculates comprehensive epidemiological analytics.
   *
   * @return {@link DiseaseAnalyticsResponse}
   */
  @Transactional(readOnly = true)
  public DiseaseAnalyticsResponse getAnalytics() {
    List<Outbreak> allOutbreaks = outbreakRepository.findAll();
    List<DiseaseReport> allReports = diseaseReportRepository.findAll();

    long totalOutbreaks = allOutbreaks.size();
    long activeOutbreaks =
        allOutbreaks.stream()
            .filter(
                o ->
                    o.getStatus() == OutbreakStatus.ACTIVE
                        || o.getStatus() == OutbreakStatus.DETECTED)
            .count();
    long resolvedOutbreaks =
        allOutbreaks.stream().filter(o -> o.getStatus() == OutbreakStatus.RESOLVED).count();
    long highRiskOutbreaks =
        allOutbreaks.stream()
            .filter(o -> o.getStatus() != OutbreakStatus.RESOLVED)
            .filter(
                o ->
                    o.getRiskScore() == OutbreakRiskScore.HIGH
                        || o.getRiskScore() == OutbreakRiskScore.CRITICAL)
            .count();

    List<Outbreak> resolvedList =
        allOutbreaks.stream()
            .filter(o -> o.getStatus() == OutbreakStatus.RESOLVED && o.getResolvedAt() != null)
            .toList();

    double avgResolutionTime =
        resolvedList.isEmpty()
            ? 0.0
            : resolvedList.stream()
                .mapToDouble(o -> ChronoUnit.HOURS.between(o.getCreatedAt(), o.getResolvedAt()))
                .average()
                .orElse(0.0);

    Map<String, Long> diseaseDistribution =
        allOutbreaks.stream()
            .collect(Collectors.groupingBy(Outbreak::getDiseaseName, Collectors.counting()));

    List<String> mostCommonDiseases =
        diseaseDistribution.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .map(Map.Entry::getKey)
            .limit(5)
            .toList();

    Map<DiagnosisConfidenceSource, Long> reportsByConfidenceSource =
        allReports.stream()
            .collect(
                Collectors.groupingBy(
                    DiseaseReport::getDiagnosisConfidenceSource, Collectors.counting()));

    return new DiseaseAnalyticsResponse(
        totalOutbreaks,
        activeOutbreaks,
        resolvedOutbreaks,
        highRiskOutbreaks,
        Math.round(avgResolutionTime * 10.0) / 10.0,
        diseaseDistribution,
        mostCommonDiseases,
        reportsByConfidenceSource);
  }
}
