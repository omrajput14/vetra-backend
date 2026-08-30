package app.vetra.disease;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import app.vetra.disease.config.DiseaseOutbreakProperties;
import app.vetra.disease.entity.OutbreakRiskScore;
import app.vetra.disease.risk.MultiSignalRiskEngine;
import app.vetra.disease.risk.RiskAssessment;
import app.vetra.disease.risk.RiskScoreProperties;
import app.vetra.disease.risk.history.HistoricalOutbreakService;
import app.vetra.disease.risk.vaccination.VaccinationGapService;
import app.vetra.disease.risk.weather.WeatherData;
import app.vetra.disease.risk.weather.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MultiSignalRiskEngineTest {

  private RiskScoreProperties properties;
  private DiseaseOutbreakProperties outbreakProperties;

  @Mock private WeatherService weatherService;
  @Mock private HistoricalOutbreakService historyService;
  @Mock private VaccinationGapService vaccinationGapService;

  private MultiSignalRiskEngine riskEngine;

  @BeforeEach
  void setUp() {
    properties = new RiskScoreProperties();
    properties.setWeightCluster(0.40);
    properties.setWeightWeather(0.20);
    properties.setWeightHistory(0.20);
    properties.setWeightVaccination(0.20);
    properties.setLowThreshold(30);
    properties.setMediumThreshold(55);
    properties.setHighThreshold(80);

    outbreakProperties = new DiseaseOutbreakProperties();

    riskEngine =
        new MultiSignalRiskEngine(
            properties,
            weatherService,
            historyService,
            vaccinationGapService,
            outbreakProperties);
  }

  @Test
  @DisplayName("Deterministic calculation: 100% cluster + 80% weather + 40% history + 70% vaccine = 78 HIGH")
  void evaluateRisk_DeterministicScoreCalculation() {
    when(weatherService.getWeatherData(anyDouble(), anyDouble()))
        .thenReturn(WeatherData.of(28.0, 75.0, 4.0, "Warm and humid"));

    when(historyService.evaluateHistory(anyString(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(
            new HistoricalOutbreakService.HistoricalSignalResult(
                40.0, 1, false, "1 past outbreak recorded"));

    when(vaccinationGapService.calculateVaccinationGap(
            anyString(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(
            new VaccinationGapService.VaccinationGapResult(
                70.0, 100, 30, 30.0, 70.0, "30% vaccinated (70% gap)"));

    // FMD with 4 confirmed cases, 2 suspected cases in 24h window
    RiskAssessment assessment =
        riskEngine.evaluateRisk(
            "Foot and Mouth Disease", 4, 2, 18.5204, 73.8567, 15.0, 24);

    assertThat(assessment).isNotNull();
    assertThat(assessment.compositeScore()).isEqualTo(78);
    assertThat(assessment.riskLevel()).isEqualTo(OutbreakRiskScore.HIGH);
    assertThat(assessment.breakdown()).isNotNull();
    assertThat(assessment.breakdown().weatherAvailable()).isTrue();
    assertThat(assessment.breakdown().clusterWeightedPoints()).isEqualTo(40.0);
    assertThat(assessment.breakdown().weatherWeightedPoints()).isEqualTo(16.0);
    assertThat(assessment.breakdown().historyWeightedPoints()).isEqualTo(8.0);
    assertThat(assessment.breakdown().vaccinationGapWeightedPoints()).isEqualTo(14.0);
  }

  @Test
  @DisplayName("Boundary testing: score is clamped between 0 and 100 and classifies CRITICAL/LOW")
  void evaluateRisk_ScoreClamping() {
    when(weatherService.getWeatherData(anyDouble(), anyDouble()))
        .thenReturn(WeatherData.of(28.0, 80.0, 10.0, "High aerosol & spore conditions"));

    when(historyService.evaluateHistory(anyString(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(
            new HistoricalOutbreakService.HistoricalSignalResult(
                100.0, 10, true, "Heavy recurrence"));

    when(vaccinationGapService.calculateVaccinationGap(
            anyString(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(
            new VaccinationGapService.VaccinationGapResult(
                100.0, 500, 0, 0.0, 100.0, "Zero vaccination"));

    // 50 confirmed cases of FMD
    RiskAssessment maxAssessment =
        riskEngine.evaluateRisk("Foot and Mouth Disease", 50, 0, 18.5204, 73.8567, 25.0, 24);

    assertThat(maxAssessment.compositeScore()).isGreaterThanOrEqualTo(95);
    assertThat(maxAssessment.compositeScore()).isLessThanOrEqualTo(100);
    assertThat(maxAssessment.riskLevel()).isEqualTo(OutbreakRiskScore.CRITICAL);

    // Zero cases
    when(weatherService.getWeatherData(anyDouble(), anyDouble()))
        .thenReturn(WeatherData.unavailable("No data"));

    when(historyService.evaluateHistory(anyString(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(
            new HistoricalOutbreakService.HistoricalSignalResult(
                0.0, 0, false, "No history"));

    when(vaccinationGapService.calculateVaccinationGap(
            anyString(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(
            new VaccinationGapService.VaccinationGapResult(
                0.0, 100, 100, 100.0, 0.0, "100% vaccinated"));

    RiskAssessment zeroAssessment =
        riskEngine.evaluateRisk("Unknown", 0, 0, 18.5204, 73.8567, 10.0, 24);

    assertThat(zeroAssessment.compositeScore()).isLessThanOrEqualTo(15);
    assertThat(zeroAssessment.riskLevel()).isEqualTo(OutbreakRiskScore.LOW);
  }

  @Test
  @DisplayName("Weather service outage: engine operates safely with fallback signal")
  void evaluateRisk_WeatherOutageFallback() {
    when(weatherService.getWeatherData(anyDouble(), anyDouble()))
        .thenReturn(WeatherData.unavailable("HTTP 503 Service Unavailable"));

    when(historyService.evaluateHistory(anyString(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(
            new HistoricalOutbreakService.HistoricalSignalResult(
                20.0, 0, false, "Baseline history"));

    when(vaccinationGapService.calculateVaccinationGap(
            anyString(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(
            new VaccinationGapService.VaccinationGapResult(
                50.0, 0, 0, 50.0, 50.0, "Baseline vaccination"));

    RiskAssessment assessment =
        riskEngine.evaluateRisk("Lumpy Skin Disease", 3, 1, 18.5204, 73.8567, 15.0, 24);

    assertThat(assessment).isNotNull();
    assertThat(assessment.breakdown().weatherAvailable()).isFalse();
    assertThat(assessment.breakdown().weatherRawScore()).isEqualTo(40.0);
    assertThat(assessment.compositeScore()).isGreaterThan(0);
  }

  @Test
  @DisplayName("Suspected vs Confirmed: Confirmed cases produce stronger signal than suspected")
  void calculateClusterSignal_SuspectedVsConfirmedDistinction() {
    // 5 confirmed cases
    double confirmedSignal =
        riskEngine.calculateClusterSignal(5, 0, 1.0, 15.0, 24);

    // 5 suspected farmer reports
    double suspectedSignal =
        riskEngine.calculateClusterSignal(0, 5, 1.0, 15.0, 24);

    assertThat(confirmedSignal).isGreaterThan(suspectedSignal);
    assertThat(suspectedSignal).isEqualTo(confirmedSignal * 0.4);
  }
}
