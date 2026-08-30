package app.vetra.disease.risk;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the Multi-Signal Livestock Health Risk Engine.
 * Supports configurable signal weights, risk thresholds, environmental caching TTL,
 * and Open-Meteo meteorological API integration parameters.
 */
@Configuration
@ConfigurationProperties(prefix = "vetra.disease.risk-engine")
public class RiskScoreProperties {

  private double weightCluster = 0.40;
  private double weightWeather = 0.20;
  private double weightHistory = 0.20;
  private double weightVaccination = 0.20;

  private int lowThreshold = 30;
  private int mediumThreshold = 55;
  private int highThreshold = 80;

  private int weatherCacheTtlMinutes = 30;
  private String weatherApiBaseUrl = "https://api.open-meteo.com/v1/forecast";
  private int weatherTimeoutSeconds = 3;
  private int weatherConnectTimeoutSeconds = 2;
  private boolean weatherEnabled = true;

  private double confirmedCaseWeight = 1.0;
  private double suspectedCaseWeight = 0.4;

  public double getWeightCluster() {
    return weightCluster;
  }

  public void setWeightCluster(double weightCluster) {
    this.weightCluster = weightCluster;
  }

  public double getWeightWeather() {
    return weightWeather;
  }

  public void setWeightWeather(double weightWeather) {
    this.weightWeather = weightWeather;
  }

  public double getWeightHistory() {
    return weightHistory;
  }

  public void setWeightHistory(double weightHistory) {
    this.weightHistory = weightHistory;
  }

  public double getWeightVaccination() {
    return weightVaccination;
  }

  public void setWeightVaccination(double weightVaccination) {
    this.weightVaccination = weightVaccination;
  }

  public int getLowThreshold() {
    return lowThreshold;
  }

  public void setLowThreshold(int lowThreshold) {
    this.lowThreshold = lowThreshold;
  }

  public int getMediumThreshold() {
    return mediumThreshold;
  }

  public void setMediumThreshold(int mediumThreshold) {
    this.mediumThreshold = mediumThreshold;
  }

  public int getHighThreshold() {
    return highThreshold;
  }

  public void setHighThreshold(int highThreshold) {
    this.highThreshold = highThreshold;
  }

  public int getWeatherCacheTtlMinutes() {
    return weatherCacheTtlMinutes;
  }

  public void setWeatherCacheTtlMinutes(int weatherCacheTtlMinutes) {
    this.weatherCacheTtlMinutes = weatherCacheTtlMinutes;
  }

  public String getWeatherApiBaseUrl() {
    return weatherApiBaseUrl;
  }

  public void setWeatherApiBaseUrl(String weatherApiBaseUrl) {
    this.weatherApiBaseUrl = weatherApiBaseUrl;
  }

  public int getWeatherTimeoutSeconds() {
    return weatherTimeoutSeconds;
  }

  public void setWeatherTimeoutSeconds(int weatherTimeoutSeconds) {
    this.weatherTimeoutSeconds = weatherTimeoutSeconds;
  }

  public int getWeatherConnectTimeoutSeconds() {
    return weatherConnectTimeoutSeconds;
  }

  public void setWeatherConnectTimeoutSeconds(int weatherConnectTimeoutSeconds) {
    this.weatherConnectTimeoutSeconds = weatherConnectTimeoutSeconds;
  }

  public boolean isWeatherEnabled() {
    return weatherEnabled;
  }

  public void setWeatherEnabled(boolean weatherEnabled) {
    this.weatherEnabled = weatherEnabled;
  }

  public double getConfirmedCaseWeight() {
    return confirmedCaseWeight;
  }

  public void setConfirmedCaseWeight(double confirmedCaseWeight) {
    this.confirmedCaseWeight = confirmedCaseWeight;
  }

  public double getSuspectedCaseWeight() {
    return suspectedCaseWeight;
  }

  public void setSuspectedCaseWeight(double suspectedCaseWeight) {
    this.suspectedCaseWeight = suspectedCaseWeight;
  }
}
