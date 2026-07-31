package app.vetra.disease.engine;

import app.vetra.disease.entity.DiagnosisStatus;
import app.vetra.disease.entity.DiseaseReport;
import app.vetra.disease.entity.Outbreak;
import app.vetra.disease.entity.OutbreakTrend;
import app.vetra.disease.geo.GeoUtils;
import app.vetra.disease.repository.DiseaseReportRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Epidemiological trend velocity analyzer comparing case velocity over sliding temporal windows.
 */
@Component
public class OutbreakTrendAnalyzer {

  private final DiseaseReportRepository diseaseReportRepository;

  /** Constructor injection. */
  public OutbreakTrendAnalyzer(DiseaseReportRepository diseaseReportRepository) {
    this.diseaseReportRepository = diseaseReportRepository;
  }

  /**
   * Evaluates and calculates the spatial-temporal velocity trend for an outbreak cluster.
   *
   * @param outbreak target outbreak cluster entity
   * @return calculated {@link OutbreakTrend}
   */
  public OutbreakTrend calculateTrend(Outbreak outbreak) {
    int windowHours = outbreak.getEvaluationWindowHours();
    Instant now = Instant.now();
    Instant currentWindowStart = now.minus(windowHours, ChronoUnit.HOURS);
    Instant previousWindowStart = now.minus(2L * windowHours, ChronoUnit.HOURS);

    List<DiseaseReport> allConfirmed = diseaseReportRepository
        .findByDiseaseNameIgnoreCaseAndDiagnosisStatusOrderByCreatedAtDesc(outbreak.getDiseaseName(), DiagnosisStatus.CONFIRMED);

    List<DiseaseReport> clusterReports = allConfirmed.stream()
        .filter(r -> GeoUtils.calculateDistanceKm(
            outbreak.getCenterLatitude(), outbreak.getCenterLongitude(), r.getLatitude(), r.getLongitude()) <= outbreak.getRadiusKm())
        .toList();

    long currentWindowCount = clusterReports.stream()
        .filter(r -> r.getCreatedAt().isAfter(currentWindowStart))
        .count();

    long previousWindowCount = clusterReports.stream()
        .filter(r -> r.getCreatedAt().isAfter(previousWindowStart) && r.getCreatedAt().isBefore(currentWindowStart))
        .count();

    if (currentWindowCount > previousWindowCount * 1.2 && currentWindowCount > previousWindowCount) {
      return OutbreakTrend.INCREASING;
    } else if (currentWindowCount < previousWindowCount * 0.8 || (currentWindowCount == 0 && previousWindowCount > 0)) {
      return OutbreakTrend.DECREASING;
    } else {
      return OutbreakTrend.STABLE;
    }
  }
}
