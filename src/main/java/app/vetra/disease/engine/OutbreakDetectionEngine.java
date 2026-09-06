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
import app.vetra.disease.risk.RiskEvaluationContext;
import app.vetra.disease.risk.SignalBreakdown;
import app.vetra.mortality.entity.AnimalMortalityEvent;
import app.vetra.mortality.enums.MortalityReportStatus;
import app.vetra.mortality.enums.MortalitySource;
import app.vetra.mortality.repository.AnimalMortalityEventRepository;
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
 * profiles, spatial clusters, multi-signal risk scoring, mortality evidence, and lifecycle
 * escalation without duplicate cluster creation.
 */
@Component
public class OutbreakDetectionEngine {

  private static final Logger log = LoggerFactory.getLogger(OutbreakDetectionEngine.class);

  private final DiseaseReportRepository diseaseReportRepository;
  private final OutbreakRepository outbreakRepository;
  private final DiseaseOutbreakProperties outbreakProperties;
  private final ApplicationEventPublisher eventPublisher;

  @Autowired(required = false)
  private AnimalMortalityEventRepository mortalityEventRepository;

  @Autowired(required = false)
  private MultiSignalRiskEngine multiSignalRiskEngine;

  /** Constructor injection with 4 parameters. */
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

  /** Setter for AnimalMortalityEventRepository injection in testing. */
  public void setMortalityEventRepository(
      AnimalMortalityEventRepository mortalityEventRepository) {
    this.mortalityEventRepository = mortalityEventRepository;
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
    processClusterEvaluation(
        diseaseName, report.getLatitude(), report.getLongitude(), profile, false);
  }

  /**
   * Evaluates an animal mortality event for spatial-temporal disease cluster correlation.
   *
   * @param event newly reported or verified mortality event
   */
  @Transactional
  public void evaluateMortalityEvent(AnimalMortalityEvent event) {
    if (event == null || event.getLatitude() == null || event.getLongitude() == null) {
      return;
    }
    String diseaseName = resolveMortalityDiseaseName(event);
    if (diseaseName == null || diseaseName.isBlank()) {
      return;
    }
    DiseaseProfile profile = outbreakProperties.getProfileForDisease(diseaseName);
    processClusterEvaluation(
        diseaseName, event.getLatitude(), event.getLongitude(), profile, true);
  }

  private void processClusterEvaluation(
      String diseaseName,
      double lat,
      double lng,
      DiseaseProfile profile,
      boolean triggerFromMortality) {
    Instant cutoffTime = Instant.now().minus(profile.evaluationWindowHours(), ChronoUnit.HOURS);
    List<DiseaseReport> confirmedReports = findNearbyReports(
        diseaseName, DiagnosisStatus.CONFIRMED, lat, lng, profile.radiusKm(), cutoffTime);
    List<DiseaseReport> suspectedReports = findNearbyReports(
        diseaseName, DiagnosisStatus.SUSPECTED, lat, lng, profile.radiusKm(), cutoffTime);
    List<AnimalMortalityEvent> mortalities =
        findNearbyMortalities(diseaseName, lat, lng, profile.radiusKm(), cutoffTime);

    int confirmedCount = confirmedReports.size();
    int suspectedCount = suspectedReports.size();
    int vetConfirmedMortality = (int) mortalities.stream()
        .filter(m -> m.getSource() == MortalitySource.VET_CONFIRMED
            && m.getStatus() == MortalityReportStatus.CONFIRMED).count();
    int farmerReportedMortality = (int) mortalities.stream()
        .filter(m -> m.getSource() == MortalitySource.FARMER_REPORTED
            && m.getStatus() != MortalityReportStatus.REJECTED).count();
    int totalMortalities = vetConfirmedMortality + farmerReportedMortality;

    ClusterEvidence evidence = new ClusterEvidence(
        confirmedCount, totalMortalities, vetConfirmedMortality, farmerReportedMortality);
    List<Outbreak> existingOutbreaks =
        findNearbyExistingOutbreaks(diseaseName, lat, lng, profile.radiusKm());

    RiskAssessment assessment = evaluateRiskAssessment(
        diseaseName, confirmedCount, suspectedCount, vetConfirmedMortality,
        farmerReportedMortality, lat, lng, profile);

    if (!existingOutbreaks.isEmpty()) {
      updateExistingCluster(existingOutbreaks.get(0), assessment, evidence);
    } else {
      int qualifyingCases = triggerFromMortality
          ? confirmedCount + vetConfirmedMortality
          : confirmedCount;
      if (qualifyingCases >= profile.minimumConfirmedCases() && qualifyingCases > 0) {
        createNewCluster(diseaseName, lat, lng, profile, assessment, evidence);
      }
    }
  }

  private List<DiseaseReport> findNearbyReports(
      String diseaseName,
      DiagnosisStatus status,
      double lat,
      double lng,
      double radiusKm,
      Instant cutoffTime) {
    return diseaseReportRepository
        .findByDiseaseNameIgnoreCaseAndDiagnosisStatusOrderByCreatedAtDesc(diseaseName, status)
        .stream()
        .filter(r -> r.getCreatedAt().isAfter(cutoffTime))
        .filter(r -> GeoUtils.calculateDistanceKm(lat, lng, r.getLatitude(), r.getLongitude())
            <= radiusKm)
        .toList();
  }

  private RiskAssessment evaluateRiskAssessment(
      String diseaseName,
      int confirmedCount,
      int suspectedCount,
      int vetConfirmedMortality,
      int farmerReportedMortality,
      double lat,
      double lng,
      DiseaseProfile profile) {
    if (multiSignalRiskEngine != null) {
      return multiSignalRiskEngine.evaluateRisk(
          new RiskEvaluationContext(
              diseaseName, confirmedCount, suspectedCount, vetConfirmedMortality,
              farmerReportedMortality, lat, lng, profile.radiusKm(),
              profile.evaluationWindowHours()));
    }
    OutbreakRiskScore fallbackScore = calculateRiskScore(
        confirmedCount, profile.severityWeight(), profile.radiusKm(),
        profile.evaluationWindowHours());
    return RiskAssessment.of(50, fallbackScore, diseaseName, null);
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

  private List<AnimalMortalityEvent> findNearbyMortalities(
      String diseaseName, double lat, double lng, double radiusKm, Instant cutoffTime) {
    if (mortalityEventRepository == null || diseaseName == null) {
      return List.of();
    }
    return mortalityEventRepository
        .findByDiseaseNameAndReportedAtAfter(diseaseName, cutoffTime)
        .stream()
        .filter(m -> m.getLatitude() != null && m.getLongitude() != null)
        .filter(
            m ->
                GeoUtils.calculateDistanceKm(lat, lng, m.getLatitude(), m.getLongitude())
                    <= radiusKm)
        .toList();
  }

  private String resolveMortalityDiseaseName(AnimalMortalityEvent event) {
    if (event.getVetDiseaseName() != null && !event.getVetDiseaseName().isBlank()) {
      return event.getVetDiseaseName().trim();
    }
    if (event.getDiseaseName() != null && !event.getDiseaseName().isBlank()) {
      return event.getDiseaseName().trim();
    }
    return null;
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
      Outbreak existing, RiskAssessment assessment, ClusterEvidence evidence) {
    OutbreakRiskScore previousRisk = existing.getRiskScore();
    existing.setAffectedReportsCount(evidence.confirmedCases());
    existing.setMortalityCount(evidence.totalMortalities());
    existing.setVetConfirmedMortalityCount(evidence.vetConfirmedMortalities());
    existing.setFarmerReportedMortalityCount(evidence.farmerReportedMortalities());
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
      publishRiskEvents(existing, previousRisk, assessment.riskLevel(), evidence.confirmedCases());
    }
  }

  private void createNewCluster(
      String diseaseName,
      double latitude,
      double longitude,
      DiseaseProfile profile,
      RiskAssessment assessment,
      ClusterEvidence evidence) {
    Outbreak.OutbreakBuilder builder =
        Outbreak.builder()
            .diseaseName(diseaseName)
            .severity(profile.reportPriority())
            .status(OutbreakStatus.ACTIVE)
            .riskScore(assessment.riskLevel())
            .compositeRiskScore(assessment.compositeScore())
            .centerLatitude(latitude)
            .centerLongitude(longitude)
            .radiusKm(profile.radiusKm())
            .affectedReportsCount(evidence.confirmedCases())
            .mortalityCount(evidence.totalMortalities())
            .vetConfirmedMortalityCount(evidence.vetConfirmedMortalities())
            .farmerReportedMortalityCount(evidence.farmerReportedMortalities())
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
        "NEW OUTBREAK CLUSTER CREATED: id={} disease='{}' risk={} compositeScore={} cases={} deaths={}",
        newOutbreak.getId(),
        diseaseName,
        assessment.riskLevel(),
        assessment.compositeScore(),
        evidence.confirmedCases(),
        evidence.totalMortalities());

    eventPublisher.publishEvent(
        new PotentialOutbreakDetectedEvent(
            diseaseName, latitude, longitude, evidence.confirmedCases()));

    eventPublisher.publishEvent(
        new OutbreakEscalatedEvent(
            newOutbreak.getId(), diseaseName, assessment.riskLevel(), evidence.confirmedCases()));
  }

  /** Structured evidence holder for outbreak cluster aggregation. */
  public record ClusterEvidence(
      int confirmedCases,
      int totalMortalities,
      int vetConfirmedMortalities,
      int farmerReportedMortalities) {}

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
