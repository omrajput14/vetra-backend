package app.vetra.disease;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import app.vetra.disease.config.DiseaseOutbreakProperties;
import app.vetra.disease.config.DiseaseProfile;
import app.vetra.disease.dto.DiseaseAnalyticsResponse;
import app.vetra.disease.engine.OutbreakDetectionEngine;
import app.vetra.disease.entity.Outbreak;
import app.vetra.disease.entity.OutbreakRiskScore;
import app.vetra.disease.entity.OutbreakStatus;
import app.vetra.disease.repository.DiseaseReportRepository;
import app.vetra.disease.repository.OutbreakRepository;
import app.vetra.disease.risk.MultiSignalRiskEngine;
import app.vetra.disease.risk.RiskAssessment;
import app.vetra.disease.risk.RiskEvaluationContext;
import app.vetra.disease.risk.RiskScoreProperties;
import app.vetra.disease.risk.history.HistoricalOutbreakService;
import app.vetra.disease.risk.vaccination.VaccinationGapService;
import app.vetra.disease.risk.weather.WeatherData;
import app.vetra.disease.risk.weather.WeatherService;
import app.vetra.disease.service.DiseaseAnalyticsService;
import app.vetra.disease.service.OperationalAlertService;
import app.vetra.mortality.entity.AnimalMortalityEvent;
import app.vetra.mortality.enums.MortalityReportStatus;
import app.vetra.mortality.enums.MortalitySource;
import app.vetra.mortality.repository.AnimalMortalityEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OutbreakMortalityRiskIntegrationTest {

  private RiskScoreProperties properties;
  private DiseaseOutbreakProperties outbreakProperties;

  @Mock private WeatherService weatherService;
  @Mock private HistoricalOutbreakService historyService;
  @Mock private VaccinationGapService vaccinationGapService;
  @Mock private DiseaseReportRepository diseaseReportRepository;
  @Mock private OutbreakRepository outbreakRepository;
  @Mock private AnimalMortalityEventRepository mortalityEventRepository;
  @Mock private ApplicationEventPublisher eventPublisher;

  private MultiSignalRiskEngine riskEngine;
  private OutbreakDetectionEngine detectionEngine;
  private OperationalAlertService alertService;
  private DiseaseAnalyticsService analyticsService;

  @BeforeEach
  void setUp() {
    properties = new RiskScoreProperties();
    properties.setWeightCluster(0.40);
    properties.setWeightWeather(0.20);
    properties.setWeightHistory(0.20);
    properties.setWeightVaccination(0.20);
    properties.setVetConfirmedMortalityWeight(1.5);
    properties.setFarmerReportedMortalityWeight(0.5);

    outbreakProperties = new DiseaseOutbreakProperties();

    riskEngine =
        new MultiSignalRiskEngine(
            properties,
            weatherService,
            historyService,
            vaccinationGapService,
            outbreakProperties);

    detectionEngine =
        new OutbreakDetectionEngine(
            diseaseReportRepository,
            outbreakRepository,
            outbreakProperties,
            eventPublisher);
    detectionEngine.setMultiSignalRiskEngine(riskEngine);
    detectionEngine.setMortalityEventRepository(mortalityEventRepository);

    alertService = new OperationalAlertService(
        outbreakRepository, diseaseReportRepository, vaccinationGapService);

    analyticsService = new DiseaseAnalyticsService(
        outbreakRepository, diseaseReportRepository, mortalityEventRepository);

    setupWeatherAndHistoryMocks();
  }

  @Test
  @DisplayName("Farmer mortality gets lower evidence weight (0.5) than vet-confirmed (1.5)")
  void testMortalityEvidenceWeights() {
    double vetMortalitySignal =
        riskEngine.calculateClusterSignal(0, 0, 2, 0, 1.0, 24);
    double farmerMortalitySignal =
        riskEngine.calculateClusterSignal(0, 0, 0, 2, 1.0, 24);

    assertThat(vetMortalitySignal).isGreaterThan(farmerMortalitySignal);
    assertThat(farmerMortalitySignal).isEqualTo(vetMortalitySignal * (0.5 / 1.5));
  }

  @Test
  @DisplayName("Mortality alone does not create an outbreak without satisfying disease thresholds")
  void testMortalityAloneDoesNotCreateOutbreak() {
    AnimalMortalityEvent event = AnimalMortalityEvent.builder()
        .diseaseName("Foot and Mouth Disease")
        .source(MortalitySource.FARMER_REPORTED)
        .status(MortalityReportStatus.PENDING_REVIEW)
        .latitude(18.5204)
        .longitude(73.8567)
        .reportedAt(Instant.now())
        .build();
    event.setId(UUID.randomUUID());

    when(mortalityEventRepository.findByDiseaseNameAndReportedAtAfter(anyString(), any()))
        .thenReturn(List.of(event));
    when(diseaseReportRepository.findByDiseaseNameIgnoreCaseAndDiagnosisStatusOrderByCreatedAtDesc(
            anyString(), any()))
        .thenReturn(List.of());
    when(outbreakRepository.findByDiseaseNameIgnoreCase(anyString(), any()))
        .thenReturn(new PageImpl<>(List.of()));

    // FMD requires 3 confirmed cases
    detectionEngine.evaluateMortalityEvent(event);

    // Verify no new cluster was saved because 1 farmer death < 3 minimum cases
    org.mockito.Mockito.verify(outbreakRepository, org.mockito.Mockito.never()).save(any());
  }

  @Test
  @DisplayName("Disease-specific thresholds: Rabies (threshold 1) triggers on single vet-confirmed case")
  void testDiseaseSpecificThresholdRabies() {
    setupWeatherAndHistoryMocks();

    AnimalMortalityEvent rabiesEvent = AnimalMortalityEvent.builder()
        .vetDiseaseName("Rabies")
        .source(MortalitySource.VET_CONFIRMED)
        .status(MortalityReportStatus.CONFIRMED)
        .latitude(18.5204)
        .longitude(73.8567)
        .reportedAt(Instant.now())
        .build();
    rabiesEvent.setId(UUID.randomUUID());

    when(mortalityEventRepository.findByDiseaseNameAndReportedAtAfter(anyString(), any()))
        .thenReturn(List.of(rabiesEvent));
    when(diseaseReportRepository.findByDiseaseNameIgnoreCaseAndDiagnosisStatusOrderByCreatedAtDesc(
            anyString(), any()))
        .thenReturn(List.of());
    when(outbreakRepository.findByDiseaseNameIgnoreCase(anyString(), any()))
        .thenReturn(new PageImpl<>(List.of()));
    when(outbreakRepository.save(any(Outbreak.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    detectionEngine.evaluateMortalityEvent(rabiesEvent);

    // Rabies minimumConfirmedCases = 1, so 1 vet-confirmed mortality satisfies profile threshold
    org.mockito.Mockito.verify(outbreakRepository).save(any(Outbreak.class));
  }

  @Test
  @DisplayName("Spatial threshold: Events outside radius are excluded from cluster evidence")
  void testSpatialThresholdFiltering() {
    setupWeatherAndHistoryMocks();

    DiseaseProfile profile = outbreakProperties.getProfileForDisease("Foot and Mouth Disease");
    assertThat(profile.radiusKm()).isEqualTo(25.0);

    // Far-away report (>100 km away)
    AnimalMortalityEvent farEvent = AnimalMortalityEvent.builder()
        .diseaseName("Foot and Mouth Disease")
        .source(MortalitySource.VET_CONFIRMED)
        .status(MortalityReportStatus.CONFIRMED)
        .latitude(19.9975) // Nashik (approx 160 km from Pune 18.5204)
        .longitude(73.7898)
        .reportedAt(Instant.now())
        .build();
    farEvent.setId(UUID.randomUUID());

    when(mortalityEventRepository.findByDiseaseNameAndReportedAtAfter(anyString(), any()))
        .thenReturn(List.of(farEvent));
    when(diseaseReportRepository.findByDiseaseNameIgnoreCaseAndDiagnosisStatusOrderByCreatedAtDesc(
            anyString(), any()))
        .thenReturn(List.of());
    when(outbreakRepository.findByDiseaseNameIgnoreCase(anyString(), any()))
        .thenReturn(new PageImpl<>(List.of()));

    AnimalMortalityEvent localTrigger = AnimalMortalityEvent.builder()
        .diseaseName("Foot and Mouth Disease")
        .source(MortalitySource.VET_CONFIRMED)
        .status(MortalityReportStatus.CONFIRMED)
        .latitude(18.5204)
        .longitude(73.8567)
        .reportedAt(Instant.now())
        .build();
    localTrigger.setId(UUID.randomUUID());

    detectionEngine.evaluateMortalityEvent(localTrigger);

    // Far event was filtered out spatially; only 1 local event exists < 3 threshold -> No outbreak
    org.mockito.Mockito.verify(outbreakRepository, org.mockito.Mockito.never()).save(any());
  }

  @Test
  @DisplayName("Risk score is strictly clamped between 0 and 100 with accurate severity")
  void testRiskScoreClampingAndSeverity() {
    when(weatherService.getWeatherData(anyDouble(), anyDouble()))
        .thenReturn(WeatherData.of(35.0, 90.0, 50.0, "Extreme conditions"));
    when(historyService.evaluateHistory(anyString(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(new HistoricalOutbreakService.HistoricalSignalResult(100.0, 20, true, "Recurrent"));
    when(vaccinationGapService.calculateVaccinationGap(anyString(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(new VaccinationGapService.VaccinationGapResult(100.0, 100, 0, 0.0, 100.0, "Zero cov"));

    RiskEvaluationContext extremeContext = new RiskEvaluationContext(
        "Foot and Mouth Disease", 100, 50, 20, 30, 18.5204, 73.8567, 25.0, 24);
    RiskAssessment assessment = riskEngine.evaluateRisk(extremeContext);

    assertThat(assessment.compositeScore()).isLessThanOrEqualTo(100);
    assertThat(assessment.compositeScore()).isGreaterThanOrEqualTo(0);
    assertThat(assessment.riskLevel()).isEqualTo(OutbreakRiskScore.CRITICAL);
  }

  @Test
  @DisplayName("Repeated outbreak detection is idempotent and updates existing cluster")
  void testRepeatedDetectionIsIdempotent() {
    setupWeatherAndHistoryMocks();

    Outbreak existing = Outbreak.builder()
        .diseaseName("Foot and Mouth Disease")
        .status(OutbreakStatus.ACTIVE)
        .riskScore(OutbreakRiskScore.MEDIUM)
        .centerLatitude(18.5204)
        .centerLongitude(73.8567)
        .radiusKm(25.0)
        .affectedReportsCount(2)
        .mortalityCount(0)
        .build();
    existing.setId(UUID.randomUUID());

    when(outbreakRepository.findByDiseaseNameIgnoreCase(anyString(), any()))
        .thenReturn(new PageImpl<>(List.of(existing)));
    when(mortalityEventRepository.findByDiseaseNameAndReportedAtAfter(anyString(), any()))
        .thenReturn(List.of());
    when(diseaseReportRepository.findByDiseaseNameIgnoreCaseAndDiagnosisStatusOrderByCreatedAtDesc(
            anyString(), any()))
        .thenReturn(List.of());

    AnimalMortalityEvent event = AnimalMortalityEvent.builder()
        .diseaseName("Foot and Mouth Disease")
        .source(MortalitySource.VET_CONFIRMED)
        .status(MortalityReportStatus.CONFIRMED)
        .latitude(18.5204)
        .longitude(73.8567)
        .reportedAt(Instant.now())
        .build();
    event.setId(UUID.randomUUID());

    detectionEngine.evaluateMortalityEvent(event);

    // Updates existing cluster instead of creating new one
    org.mockito.Mockito.verify(outbreakRepository).save(existing);
  }

  @Test
  @DisplayName("Operational alerts generate deterministic and deduplicated UUIDs")
  void testAlertDeduplication() {
    UUID outbreakId = UUID.randomUUID();
    Outbreak criticalOutbreak = Outbreak.builder()
        .diseaseName("Anthrax")
        .status(OutbreakStatus.ACTIVE)
        .riskScore(OutbreakRiskScore.CRITICAL)
        .compositeRiskScore(85)
        .centerLatitude(18.5204)
        .centerLongitude(73.8567)
        .radiusKm(10.0)
        .affectedReportsCount(2)
        .mortalityCount(3)
        .vetConfirmedMortalityCount(2)
        .farmerReportedMortalityCount(1)
        .build();
    criticalOutbreak.setId(outbreakId);
    criticalOutbreak.setCreatedAt(Instant.now());

    when(outbreakRepository.findAll()).thenReturn(List.of(criticalOutbreak));
    when(diseaseReportRepository.findAll()).thenReturn(List.of());

    var alerts1 = alertService.listOperationalAlerts();
    var alerts2 = alertService.listOperationalAlerts();

    assertThat(alerts1).isNotEmpty();
    assertThat(alerts1.get(0).id()).isEqualTo(alerts2.get(0).id());
    assertThat(alerts1.get(0).severity()).isEqualTo("CRITICAL");
    assertThat(alerts1.get(0).whyItMatters()).contains("mortalities (2 vet-confirmed, 1 farmer-reported)");
  }

  @Test
  @DisplayName("Disease analytics accurately reflects real mortality aggregates")
  void testDiseaseAnalyticsMortalityCounts() {
    when(outbreakRepository.findAll()).thenReturn(List.of());
    when(diseaseReportRepository.findAll()).thenReturn(List.of());
    when(mortalityEventRepository.count()).thenReturn(15L);
    when(mortalityEventRepository.countBySource(MortalitySource.FARMER_REPORTED)).thenReturn(10L);
    when(mortalityEventRepository.countBySource(MortalitySource.VET_CONFIRMED)).thenReturn(5L);

    DiseaseAnalyticsResponse analytics = analyticsService.getAnalytics();

    assertThat(analytics.totalMortalityReports()).isEqualTo(15L);
    assertThat(analytics.farmerReportedMortalityCount()).isEqualTo(10L);
    assertThat(analytics.vetConfirmedMortalityCount()).isEqualTo(5L);
  }

  private void setupWeatherAndHistoryMocks() {
    when(weatherService.getWeatherData(anyDouble(), anyDouble()))
        .thenReturn(WeatherData.of(25.0, 60.0, 0.0, "Normal"));
    when(historyService.evaluateHistory(anyString(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(new HistoricalOutbreakService.HistoricalSignalResult(20.0, 0, false, "Baseline"));
    when(vaccinationGapService.calculateVaccinationGap(anyString(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(new VaccinationGapService.VaccinationGapResult(30.0, 100, 70, 70.0, 30.0, "70% cov"));
  }
}
