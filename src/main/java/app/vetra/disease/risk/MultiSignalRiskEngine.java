package app.vetra.disease.risk;

import app.vetra.disease.config.DiseaseOutbreakProperties;
import app.vetra.disease.config.DiseaseProfile;
import app.vetra.disease.entity.OutbreakRiskScore;
import app.vetra.disease.risk.history.HistoricalOutbreakService;
import app.vetra.disease.risk.vaccination.VaccinationGapService;
import app.vetra.disease.risk.weather.WeatherData;
import app.vetra.disease.risk.weather.WeatherService;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Multi-Signal Livestock Health Risk Engine.
 * Evaluates spatial-temporal clusters, meteorological conditions, historical patterns,
 * and herd vaccination immunity gaps into a deterministic, explainable 0–100 risk score.
 */
@Component
public class MultiSignalRiskEngine {

  private final RiskScoreProperties properties;
  private final WeatherService weatherService;
  private final HistoricalOutbreakService historyService;
  private final VaccinationGapService vaccinationGapService;
  private final DiseaseOutbreakProperties outbreakProperties;

  /** Constructor injection. */
  public MultiSignalRiskEngine(
      RiskScoreProperties properties,
      WeatherService weatherService,
      HistoricalOutbreakService historyService,
      VaccinationGapService vaccinationGapService,
      DiseaseOutbreakProperties outbreakProperties) {
    this.properties = properties;
    this.weatherService = weatherService;
    this.historyService = historyService;
    this.vaccinationGapService = vaccinationGapService;
    this.outbreakProperties = outbreakProperties;
  }

  /**
   * Evaluates composite multi-signal risk for a disease cluster using a risk evaluation context.
   *
   * @param ctx evaluation context encapsulating cases, mortalities, and geographic parameters
   * @return {@link RiskAssessment}
   */
  public RiskAssessment evaluateRisk(RiskEvaluationContext ctx) {
    DiseaseProfile profile = outbreakProperties.getProfileForDisease(ctx.diseaseName());

    double clusterRaw =
        calculateClusterSignal(
            ctx.confirmedCases(),
            ctx.suspectedCases(),
            ctx.vetConfirmedMortalities(),
            ctx.farmerReportedMortalities(),
            profile.severityWeight(),
            ctx.windowHours());

    WeatherData weather = weatherService.getWeatherData(ctx.latitude(), ctx.longitude());
    double weatherRaw = calculateWeatherSignal(ctx.diseaseName(), weather);

    HistoricalOutbreakService.HistoricalSignalResult history =
        historyService.evaluateHistory(
            ctx.diseaseName(), ctx.latitude(), ctx.longitude(), ctx.radiusKm());
    double historyRaw = history.normalizedScore();

    VaccinationGapService.VaccinationGapResult vaccine =
        vaccinationGapService.calculateVaccinationGap(
            ctx.diseaseName(), ctx.latitude(), ctx.longitude(), ctx.radiusKm());
    double vaccineGapRaw = vaccine.normalizedScore();

    double clusterWeighted = clusterRaw * properties.getWeightCluster();
    double weatherWeighted = weatherRaw * properties.getWeightWeather();
    double historyWeighted = historyRaw * properties.getWeightHistory();
    double vaccineWeighted = vaccineGapRaw * properties.getWeightVaccination();

    double rawTotal = clusterWeighted + weatherWeighted + historyWeighted + vaccineWeighted;
    int compositeScore = (int) Math.round(Math.min(100.0, Math.max(0.0, rawTotal)));

    OutbreakRiskScore riskLevel = classifyRiskLevel(compositeScore);

    String mortalityPart = "";
    if (ctx.vetConfirmedMortalities() > 0 || ctx.farmerReportedMortalities() > 0) {
      mortalityPart =
          String.format(
              Locale.ROOT,
              ", %d vet-confirmed + %d farmer-reported death(s)",
              ctx.vetConfirmedMortalities(),
              ctx.farmerReportedMortalities());
    }

    String explanation =
        String.format(
            Locale.ROOT,
            "Evaluated with %d confirmed + %d suspected case(s)%s (%.1f pts). Weather: %s (%.1f pts). History: %s (%.1f pts). Herd Immunity: %s (%.1f pts).",
            ctx.confirmedCases(),
            ctx.suspectedCases(),
            mortalityPart,
            clusterWeighted,
            weather.available() ? weather.statusDescription() : "Unavailable",
            weatherWeighted,
            history.explanation(),
            historyWeighted,
            vaccine.explanation(),
            vaccineWeighted);

    String recommendation = generateRecommendation(riskLevel, ctx.diseaseName(), ctx.radiusKm());

    SignalBreakdown breakdown =
        new SignalBreakdown(
            Math.round(clusterRaw * 10.0) / 10.0,
            Math.round(clusterWeighted * 10.0) / 10.0,
            Math.round(weatherRaw * 10.0) / 10.0,
            Math.round(weatherWeighted * 10.0) / 10.0,
            Math.round(historyRaw * 10.0) / 10.0,
            Math.round(historyWeighted * 10.0) / 10.0,
            Math.round(vaccineGapRaw * 10.0) / 10.0,
            Math.round(vaccineWeighted * 10.0) / 10.0,
            weather.temperatureCelsius(),
            weather.relativeHumidityPercent(),
            weather.precipitationMm(),
            weather.available(),
            vaccine.coveragePercentage(),
            vaccine.totalEligibleAnimals(),
            vaccine.vaccinatedAnimals(),
            history.pastOutbreaksCount(),
            history.seasonalPeak(),
            explanation,
            recommendation);

    return RiskAssessment.of(compositeScore, riskLevel, ctx.diseaseName(), breakdown);
  }

  /**
   * Evaluates composite multi-signal risk for a disease cluster (backward-compatible convenience
   * method).
   *
   * @param diseaseName target disease
   * @param confirmedCases count of confirmed veterinary reports
   * @param suspectedCases count of unverified farmer reports
   * @param latitude cluster center latitude
   * @param longitude cluster center longitude
   * @param radiusKm containment radius in km
   * @param windowHours evaluation window in hours
   * @return {@link RiskAssessment}
   */
  public RiskAssessment evaluateRisk(
      String diseaseName,
      int confirmedCases,
      int suspectedCases,
      double latitude,
      double longitude,
      double radiusKm,
      int windowHours) {
    return evaluateRisk(
        new RiskEvaluationContext(
            diseaseName,
            confirmedCases,
            suspectedCases,
            0,
            0,
            latitude,
            longitude,
            radiusKm,
            windowHours));
  }

  /**
   * Calculates Signal 1: Cluster Velocity & Density raw score including mortality evidence.
   *
   * @param confirmedCases confirmed live cases
   * @param suspectedCases suspected live cases
   * @param vetConfirmedMortalities vet-confirmed mortality count
   * @param farmerReportedMortalities farmer-reported mortality count
   * @param severityWeight disease-specific severity multiplier
   * @param windowHours evaluation window in hours
   * @return normalized 0–100 raw signal score
   */
  public double calculateClusterSignal(
      int confirmedCases,
      int suspectedCases,
      int vetConfirmedMortalities,
      int farmerReportedMortalities,
      double severityWeight,
      int windowHours) {
    if (confirmedCases <= 0
        && suspectedCases <= 0
        && vetConfirmedMortalities <= 0
        && farmerReportedMortalities <= 0) {
      return 0.0;
    }

    double weightedCases =
        (confirmedCases * properties.getConfirmedCaseWeight())
            + (suspectedCases * properties.getSuspectedCaseWeight())
            + (vetConfirmedMortalities * properties.getVetConfirmedMortalityWeight())
            + (farmerReportedMortalities * properties.getFarmerReportedMortalityWeight());

    double velocityFactor = Math.min(2.0, 24.0 / Math.max(1, windowHours));
    double raw = weightedCases * severityWeight * velocityFactor * 12.5;

    return Math.min(100.0, Math.max(0.0, raw));
  }

  /** Backward-compatible 5-parameter cluster signal calculation. */
  public double calculateClusterSignal(
      int confirmedCases,
      int suspectedCases,
      double severityWeight,
      double radiusKm,
      int windowHours) {
    return calculateClusterSignal(
        confirmedCases, suspectedCases, 0, 0, severityWeight, windowHours);
  }

  public double calculateWeatherSignal(String diseaseName, WeatherData weather) {
    if (!weather.available() || weather.temperatureCelsius() == null || weather.relativeHumidityPercent() == null) {
      return 40.0;
    }

    double temp = weather.temperatureCelsius();
    double humidity = weather.relativeHumidityPercent();
    double rain = weather.precipitationMm() != null ? weather.precipitationMm() : 0.0;
    String key = diseaseName != null ? diseaseName.toLowerCase() : "";

    if (isAerosolDisease(key)) {
      return calculateAerosolWeatherScore(temp, humidity);
    }
    if (isVectorDisease(key)) {
      return calculateVectorWeatherScore(temp, humidity, rain);
    }
    if (isSporeDisease(key)) {
      return calculateSporeWeatherScore(temp, rain);
    }

    return calculateDefaultWeatherScore(temp, humidity);
  }

  private boolean isAerosolDisease(String key) {
    return key.contains("foot") || key.contains("fmd") || key.contains("respiratory");
  }

  private boolean isVectorDisease(String key) {
    return key.contains("lumpy") || key.contains("lsd") || key.contains("blue") || key.contains("vector");
  }

  private boolean isSporeDisease(String key) {
    return key.contains("anthrax") || key.contains("blackleg") || key.contains("bq");
  }

  private double calculateDefaultWeatherScore(double temp, double humidity) {
    double baseline = (humidity > 60 ? 25.0 : 10.0) + (temp > 20 ? 25.0 : 10.0);
    return Math.min(100.0, baseline);
  }

  private double calculateAerosolWeatherScore(double temp, double humidity) {
    double humidityFactor = humidity > 70 ? (humidity / 100.0) * 60.0 : 30.0;
    double tempFactor = (temp >= 15.0 && temp <= 32.0) ? 35.0 : 15.0;
    return Math.min(100.0, humidityFactor + tempFactor);
  }

  private double calculateVectorWeatherScore(double temp, double humidity, double rain) {
    double rainFactor = rain > 1.0 ? 40.0 : 15.0;
    double tempFactor = (temp >= 22.0 && temp <= 36.0) ? 35.0 : 15.0;
    double humidityFactor = humidity > 60 ? 25.0 : 10.0;
    return Math.min(100.0, rainFactor + tempFactor + humidityFactor);
  }

  private double calculateSporeWeatherScore(double temp, double rain) {
    double rainFactor = rain > 5.0 ? 50.0 : 20.0;
    double tempFactor = temp > 25.0 ? 40.0 : 20.0;
    return Math.min(100.0, rainFactor + tempFactor);
  }

  public OutbreakRiskScore classifyRiskLevel(int score) {
    if (score >= properties.getHighThreshold()) {
      return OutbreakRiskScore.CRITICAL;
    }
    if (score >= properties.getMediumThreshold()) {
      return OutbreakRiskScore.HIGH;
    }
    if (score >= properties.getLowThreshold()) {
      return OutbreakRiskScore.MEDIUM;
    }
    return OutbreakRiskScore.LOW;
  }

  private String generateRecommendation(
      OutbreakRiskScore level, String diseaseName, double radiusKm) {
    return switch (level) {
      case CRITICAL -> String.format(
          Locale.ROOT,
          "CRITICAL SURVEILLANCE: Enforce strict bio-containment within %.1f km. Initiate emergency ring vaccination and restrict livestock movement.",
          radiusKm);
      case HIGH -> String.format(
          Locale.ROOT,
          "HIGH RISK: Enhanced clinical surveillance recommended for %s. Inspect local herds for early lesions and verify booster vaccination records.",
          diseaseName);
      case MEDIUM -> "ADVISORY MONITORING: Inform local livestock farmers on biosecurity hygiene, clean water sources, and pasture separation.";
      case LOW -> "ROUTINE SURVEILLANCE: Normal preventative health monitoring active. No emergency containment required.";
    };
  }
}
