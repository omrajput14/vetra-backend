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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Intelligent Outbreak Detection Engine. Evaluates temporal windows, disease-specific threshold
 * profiles, spatial clusters, risk scoring, and lifecycle escalation without duplicate cluster
 * creation.
 */
@Component
public class OutbreakDetectionEngine {

  private static final Logger log = LoggerFactory.getLogger(OutbreakDetectionEngine.class);

  private final DiseaseReportRepository diseaseReportRepository;
  private final OutbreakRepository outbreakRepository;
  private final DiseaseOutbreakProperties outbreakProperties;
  private final ApplicationEventPublisher eventPublisher;

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

    List<DiseaseReport> timeWindowReports =
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

    int caseCount = timeWindowReports.size();
    List<Outbreak> existingOutbreaks =
        findNearbyExistingOutbreaks(
            diseaseName, report.getLatitude(), report.getLongitude(), profile.radiusKm());

    OutbreakRiskScore calculatedRisk =
        calculateRiskScore(
            caseCount,
            profile.severityWeight(),
            profile.radiusKm(),
            profile.evaluationWindowHours());

    if (!existingOutbreaks.isEmpty()) {
      updateExistingCluster(existingOutbreaks.get(0), calculatedRisk, caseCount);
    } else if (caseCount >= profile.minimumConfirmedCases()) {
      createNewCluster(report, profile, calculatedRisk, caseCount);
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
      Outbreak existing, OutbreakRiskScore calculatedRisk, int caseCount) {
    OutbreakRiskScore previousRisk = existing.getRiskScore();
    existing.setAffectedReportsCount(caseCount);
    existing.setLastCaseReportedAt(Instant.now());
    existing.setRiskScore(calculatedRisk);
    existing.setStatus(OutbreakStatus.ACTIVE);
    outbreakRepository.save(existing);

    if (previousRisk != calculatedRisk) {
      publishRiskEvents(existing, previousRisk, calculatedRisk, caseCount);
    }
  }

  private void createNewCluster(
      DiseaseReport report,
      DiseaseProfile profile,
      OutbreakRiskScore calculatedRisk,
      int caseCount) {
    Outbreak newOutbreak =
        Outbreak.builder()
            .diseaseName(report.getDiseaseName())
            .severity(profile.reportPriority())
            .status(OutbreakStatus.ACTIVE)
            .riskScore(calculatedRisk)
            .centerLatitude(report.getLatitude())
            .centerLongitude(report.getLongitude())
            .radiusKm(profile.radiusKm())
            .affectedReportsCount(caseCount)
            .evaluationWindowHours(profile.evaluationWindowHours())
            .lastCaseReportedAt(Instant.now())
            .build();

    newOutbreak = outbreakRepository.save(newOutbreak);

    log.warn(
        "NEW OUTBREAK CLUSTER CREATED: id={} disease='{}' risk={} cases={}",
        newOutbreak.getId(),
        report.getDiseaseName(),
        calculatedRisk,
        caseCount);

    eventPublisher.publishEvent(
        new PotentialOutbreakDetectedEvent(
            report.getDiseaseName(), report.getLatitude(), report.getLongitude(), caseCount));

    eventPublisher.publishEvent(
        new OutbreakEscalatedEvent(
            newOutbreak.getId(), report.getDiseaseName(), calculatedRisk, caseCount));
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
