package app.vetra.disease.engine;

import app.vetra.disease.config.DiseaseOutbreakProperties;
import app.vetra.disease.config.DiseaseProfile;
import app.vetra.disease.entity.DiagnosisStatus;
import app.vetra.disease.entity.DiseaseReport;
import app.vetra.disease.entity.Outbreak;
import app.vetra.disease.entity.OutbreakRiskScore;
import app.vetra.disease.entity.OutbreakStatus;
import app.vetra.disease.event.OutbreakDeEscalatedEvent;
import app.vetra.disease.event.OutbreakEscalatedEvent;
import app.vetra.disease.event.OutbreakRiskChangedEvent;
import app.vetra.disease.event.PotentialOutbreakDetectedEvent;
import app.vetra.disease.geo.GeoUtils;
import app.vetra.disease.repository.DiseaseReportRepository;
import app.vetra.disease.repository.OutbreakRepository;
import app.vetra.disease.risk.MultiSignalRiskEngine;
import app.vetra.disease.risk.RiskAssessment;
import app.vetra.disease.risk.SignalBreakdown;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Intelligent Outbreak Detection Engine. Evaluates temporal windows, disease-specific threshold
 * profiles, spatial clusters, multi-signal risk scoring, and lifecycle escalation without duplicate
 * cluster creation.
 */
@Component
public class OutbreakDetectionEngine {

  private static final Logger log = LoggerFactory.getLogger(OutbreakDetectionEngine.class);

  private final DiseaseReportRepository diseaseReportRepository;
  private final OutbreakRepository outbreakRepository;
  private final DiseaseOutbreakProperties outbreakProperties;
  private final ApplicationEventPublisher eventPublisher;

  @Autowired(required = false)
  private MultiSignalRiskEngine multiSignalRiskEngine;

  /** Constructor injection. */
  public OutbreakDetectionEngine(
      DiseaseReportRepository diseaseReportRepository,
      OutbreakRepository outbreakRepository,
      DiseaseOutbreakProperties outbreakProperties,
      ApplicationEventPublisher eventPublisher) {
    this.diseaseReportRepository = diseaseReportRepository;
    this.outbreakRepository = outbreakRepository;
    this.outbreakProperties = outbreakProperties;
    this.eventPublisher = eventPublisher;
  }

  /** Setter for MultiSignalRiskEngine injection in testing. */
  public void setMultiSignalRiskEngine(MultiSignalRiskEngine multiSignalRiskEngine) {
    this.multiSignalRiskEngine = multiSignalRiskEngine;
  }

  /**
   * Evaluates a disease report for spatial and temporal outbreak cluster detection.
   *
   * @param report newly submitted or updated disease report
   */
  @Transactional
  public void evaluateReport(DiseaseReport report) {
    if (report.getDiagnosisStatus() != DiagnosisStatus.CONFIRMED) {
      return;
    }

    String diseaseName = report.getDiseaseName();
    DiseaseProfile profile = outbreakProperties.getProfileForDisease(diseaseName);
    Instant cutoffTime = Instant.now().minus(profile.evaluationWindowHours(), ChronoUnit.HOURS);

    List<DiseaseReport> confirmedReports =
        diseaseReportRepository
            .findByDiseaseNameIgnoreCaseAndDiagnosisStatusOrderByCreatedAtDesc(
                diseaseName, DiagnosisStatus.CONFIRMED)
            .stream()
            .filter(r -> r.getCreatedAt().isAfter(cutoffTime))
            .filter(
                r ->
                    GeoUtils.calculateDistanceKm(
                            report.getLatitude(),
                            report.getLongitude(),
                            r.getLatitude(),
                            r.getLongitude())
                        <= profile.radiusKm())
            .toList();

    List<DiseaseReport> suspectedReports =
        diseaseReportRepository
            .findByDiseaseNameIgnoreCaseAndDiagnosisStatusOrderByCreatedAtDesc(
                diseaseName, DiagnosisStatus.SUSPECTED)
            .stream()
            .filter(r -> r.getCreatedAt().isAfter(cutoffTime))
            .filter(
                r ->
                    GeoUtils.calculateDistanceKm(
                            report.getLatitude(),
                            report.getLongitude(),
                            r.getLatitude(),
                            r.getLongitude())
                        <= profile.radiusKm())
            .toList();

    int confirmedCount = confirmedReports.size();
    int suspectedCount = suspectedReports.size();

    List<Outbreak> existingOutbreaks =
        findNearbyExistingOutbreaks(
            diseaseName, report.getLatitude(), report.getLongitude(), profile.radiusKm());

    RiskAssessment assessment;
    if (multiSignalRiskEngine != null) {
      assessment =
          multiSignalRiskEngine.evaluateRisk(
              diseaseName,
              confirmedCount,
              suspectedCount,
              report.getLatitude(),
              report.getLongitude(),
              profile.radiusKm(),
              profile.evaluationWindowHours());
    } else {
      OutbreakRiskScore fallbackScore =
          calculateRiskScore(
              confirmedCount,
              profile.severityWeight(),
              profile.radiusKm(),
              profile.evaluationWindowHours());
      assessment = RiskAssessment.of(50, fallbackScore, diseaseName, null);
    }

    if (!existingOutbreaks.isEmpty()) {
      updateExistingCluster(existingOutbreaks.get(0), assessment, confirmedCount);
    } else if (confirmedCount >= profile.minimumConfirmedCases()) {
      createNewCluster(report, profile, assessment, confirmedCount);
    }
  }

  /**
   * Calculates epidemiological risk severity score based on case count, severity weight, and
   * velocity.
   */
  public OutbreakRiskScore calculateRiskScore(
      int caseCount, double severityWeight, double radiusKm, int windowHours) {
    if (caseCount <= 0) {
      return OutbreakRiskScore.LOW;
    }

    double velocityFactor = 24.0 / Math.max(1, windowHours);
    double score = caseCount * severityWeight * velocityFactor;

    if (score >= 6.0 || caseCount >= 10) {
      return OutbreakRiskScore.CRITICAL;
    } else if (score >= 3.5 || caseCount >= 5) {
      return OutbreakRiskScore.HIGH;
    } else if (score >= 1.5 || caseCount >= 3) {
      return OutbreakRiskScore.MEDIUM;
    } else {
      return OutbreakRiskScore.LOW;
    }
  }

  private List<Outbreak> findNearbyExistingOutbreaks(
      String diseaseName, double lat, double lng, double radiusKm) {
    return outbreakRepository.findByDiseaseNameIgnoreCase(diseaseName, null).getContent().stream()
        .filter(o -> o.getStatus() != OutbreakStatus.RESOLVED)
        .filter(
            o ->
                GeoUtils.calculateDistanceKm(
                        lat, lng, o.getCenterLatitude(), o.getCenterLongitude())
                    <= radiusKm)
            .toList();
  }

  private void updateExistingCluster(
      Outbreak existing, RiskAssessment assessment, int caseCount) {
    OutbreakRiskScore previousRisk = existing.getRiskScore();
    existing.setAffectedReportsCount(caseCount);
    existing.setLastCaseReportedAt(Instant.now());
    existing.setRiskScore(assessment.riskLevel());
    existing.setCompositeRiskScore(assessment.compositeScore());
    existing.setStatus(OutbreakStatus.ACTIVE);

    SignalBreakdown bd = assessment.breakdown();
    if (bd != null) {
      existing.setClusterScore(bd.clusterRawScore());
      existing.setWeatherScore(bd.weatherRawScore());
      existing.setHistoryScore(bd.historyRawScore());
      existing.setVaccinationGapScore(bd.vaccinationGapRawScore());
      existing.setWeatherTemperature(bd.weatherTemperature());
      existing.setWeatherHumidity(bd.weatherHumidity());
      existing.setWeatherPrecipitation(bd.weatherPrecipitation());
      existing.setVaccinationCoveragePct(bd.vaccinationCoveragePct());
      existing.setRiskExplanation(bd.explanation());
      existing.setRecommendedAction(bd.recommendedAction());
    }

    outbreakRepository.save(existing);

    if (previousRisk != assessment.riskLevel()) {
      publishRiskEvents(existing, previousRisk, assessment.riskLevel(), caseCount);
    }
  }

  private void createNewCluster(
      DiseaseReport report,
      DiseaseProfile profile,
      RiskAssessment assessment,
      int caseCount) {
    Outbreak.OutbreakBuilder builder =
        Outbreak.builder()
            .diseaseName(report.getDiseaseName())
            .severity(profile.reportPriority())
            .status(OutbreakStatus.ACTIVE)
            .riskScore(assessment.riskLevel())
            .compositeRiskScore(assessment.compositeScore())
            .centerLatitude(report.getLatitude())
            .centerLongitude(report.getLongitude())
            .radiusKm(profile.radiusKm())
            .affectedReportsCount(caseCount)
            .evaluationWindowHours(profile.evaluationWindowHours())
            .lastCaseReportedAt(Instant.now());

    SignalBreakdown bd = assessment.breakdown();
    if (bd != null) {
      builder
          .clusterScore(bd.clusterRawScore())
          .weatherScore(bd.weatherRawScore())
          .historyScore(bd.historyRawScore())
          .vaccinationGapScore(bd.vaccinationGapRawScore())
          .weatherTemperature(bd.weatherTemperature())
          .weatherHumidity(bd.weatherHumidity())
          .weatherPrecipitation(bd.weatherPrecipitation())
          .vaccinationCoveragePct(bd.vaccinationCoveragePct())
          .riskExplanation(bd.explanation())
          .recommendedAction(bd.recommendedAction());
    }

    Outbreak newOutbreak = outbreakRepository.save(builder.build());

    log.warn(
        "NEW OUTBREAK CLUSTER CREATED: id={} disease='{}' risk={} compositeScore={} cases={}",
        newOutbreak.getId(),
        report.getDiseaseName(),
        assessment.riskLevel(),
        assessment.compositeScore(),
        caseCount);

    eventPublisher.publishEvent(
        new PotentialOutbreakDetectedEvent(
            report.getDiseaseName(), report.getLatitude(), report.getLongitude(), caseCount));

    eventPublisher.publishEvent(
        new OutbreakEscalatedEvent(
            newOutbreak.getId(), report.getDiseaseName(), assessment.riskLevel(), caseCount));
  }

  private void publishRiskEvents(
      Outbreak outbreak, OutbreakRiskScore oldRisk, OutbreakRiskScore newRisk, int caseCount) {
    eventPublisher.publishEvent(
        new OutbreakRiskChangedEvent(
            outbreak.getId(), outbreak.getDiseaseName(), oldRisk, newRisk));

    if (newRisk.ordinal() > oldRisk.ordinal()) {
      eventPublisher.publishEvent(
          new OutbreakEscalatedEvent(
              outbreak.getId(), outbreak.getDiseaseName(), newRisk, caseCount));
    } else if (newRisk.ordinal() < oldRisk.ordinal()) {
      eventPublisher.publishEvent(
          new OutbreakDeEscalatedEvent(outbreak.getId(), outbreak.getDiseaseName(), newRisk));
    }
  }
}
