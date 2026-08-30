package app.vetra.disease.entity;

import app.vetra.infrastructure.persistence.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

/** Entity representing a disease outbreak cluster under active surveillance. */
@Entity
@Table(name = "outbreaks")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Outbreak extends BaseEntity {

  @Column(name = "disease_name", nullable = false, length = 128)
  private String diseaseName;

  @Column(name = "severity", nullable = false, length = 32)
  @Builder.Default
  private String severity = "MEDIUM";

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  @Builder.Default
  private OutbreakStatus status = OutbreakStatus.ACTIVE;

  @Enumerated(EnumType.STRING)
  @Column(name = "risk_score", nullable = false, length = 32)
  @Builder.Default
  private OutbreakRiskScore riskScore = OutbreakRiskScore.MEDIUM;

  @Enumerated(EnumType.STRING)
  @Column(name = "trend", nullable = false, length = 32)
  @Builder.Default
  private OutbreakTrend trend = OutbreakTrend.STABLE;

  @Column(name = "center_latitude", nullable = false)
  private Double centerLatitude;

  @Column(name = "center_longitude", nullable = false)
  private Double centerLongitude;

  @Column(name = "radius_km", nullable = false)
  @Builder.Default
  private Double radiusKm = 10.0;

  @Column(name = "affected_reports_count", nullable = false)
  @Builder.Default
  private Integer affectedReportsCount = 0;

  @Column(name = "evaluation_window_hours", nullable = false)
  @Builder.Default
  private Integer evaluationWindowHours = 72;

  @Column(name = "last_case_reported_at")
  @Builder.Default
  private Instant lastCaseReportedAt = Instant.now();

  @Column(name = "last_evaluated_at")
  @Builder.Default
  private Instant lastEvaluatedAt = Instant.now();

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  @Column(name = "resolution_reason", length = 128)
  private String resolutionReason;

  // Multi-Signal Risk Intelligence Fields
  @Column(name = "composite_risk_score")
  @Builder.Default
  private Integer compositeRiskScore = 50;

  @Column(name = "cluster_score")
  private Double clusterScore;

  @Column(name = "weather_score")
  private Double weatherScore;

  @Column(name = "history_score")
  private Double historyScore;

  @Column(name = "vaccination_gap_score")
  private Double vaccinationGapScore;

  @Column(name = "weather_temperature")
  private Double weatherTemperature;

  @Column(name = "weather_humidity")
  private Double weatherHumidity;

  @Column(name = "weather_precipitation")
  private Double weatherPrecipitation;

  @Column(name = "vaccination_coverage_pct")
  private Double vaccinationCoveragePct;

  @Column(name = "risk_explanation", columnDefinition = "TEXT")
  private String riskExplanation;

  @Column(name = "recommended_action", columnDefinition = "TEXT")
  private String recommendedAction;

  public String getDiseaseName() {
    return diseaseName;
  }

  public void setDiseaseName(String diseaseName) {
    this.diseaseName = diseaseName;
  }

  public String getSeverity() {
    return severity;
  }

  public void setSeverity(String severity) {
    this.severity = severity;
  }

  public OutbreakStatus getStatus() {
    return status;
  }

  public void setStatus(OutbreakStatus status) {
    this.status = status;
  }

  public OutbreakRiskScore getRiskScore() {
    return riskScore;
  }

  public void setRiskScore(OutbreakRiskScore riskScore) {
    this.riskScore = riskScore;
  }

  public OutbreakTrend getTrend() {
    return trend;
  }

  public void setTrend(OutbreakTrend trend) {
    this.trend = trend;
  }

  public Double getCenterLatitude() {
    return centerLatitude;
  }

  public void setCenterLatitude(Double centerLatitude) {
    this.centerLatitude = centerLatitude;
  }

  public Double getCenterLongitude() {
    return centerLongitude;
  }

  public void setCenterLongitude(Double centerLongitude) {
    this.centerLongitude = centerLongitude;
  }

  public Double getRadiusKm() {
    return radiusKm;
  }

  public void setRadiusKm(Double radiusKm) {
    this.radiusKm = radiusKm;
  }

  public Integer getAffectedReportsCount() {
    return affectedReportsCount;
  }

  public void setAffectedReportsCount(Integer affectedReportsCount) {
    this.affectedReportsCount = affectedReportsCount;
  }

  public Integer getEvaluationWindowHours() {
    return evaluationWindowHours;
  }

  public void setEvaluationWindowHours(Integer evaluationWindowHours) {
    this.evaluationWindowHours = evaluationWindowHours;
  }

  public Instant getLastCaseReportedAt() {
    return lastCaseReportedAt;
  }

  public void setLastCaseReportedAt(Instant lastCaseReportedAt) {
    this.lastCaseReportedAt = lastCaseReportedAt;
  }

  public Instant getLastEvaluatedAt() {
    return lastEvaluatedAt;
  }

  public void setLastEvaluatedAt(Instant lastEvaluatedAt) {
    this.lastEvaluatedAt = lastEvaluatedAt;
  }

  public Instant getResolvedAt() {
    return resolvedAt;
  }

  public void setResolvedAt(Instant resolvedAt) {
    this.resolvedAt = resolvedAt;
  }

  public String getResolutionReason() {
    return resolutionReason;
  }

  public void setResolutionReason(String resolutionReason) {
    this.resolutionReason = resolutionReason;
  }

  public Integer getCompositeRiskScore() {
    return compositeRiskScore != null ? compositeRiskScore : 50;
  }

  public void setCompositeRiskScore(Integer compositeRiskScore) {
    this.compositeRiskScore = compositeRiskScore;
  }

  public Double getClusterScore() {
    return clusterScore;
  }

  public void setClusterScore(Double clusterScore) {
    this.clusterScore = clusterScore;
  }

  public Double getWeatherScore() {
    return weatherScore;
  }

  public void setWeatherScore(Double weatherScore) {
    this.weatherScore = weatherScore;
  }

  public Double getHistoryScore() {
    return historyScore;
  }

  public void setHistoryScore(Double historyScore) {
    this.historyScore = historyScore;
  }

  public Double getVaccinationGapScore() {
    return vaccinationGapScore;
  }

  public void setVaccinationGapScore(Double vaccinationGapScore) {
    this.vaccinationGapScore = vaccinationGapScore;
  }

  public Double getWeatherTemperature() {
    return weatherTemperature;
  }

  public void setWeatherTemperature(Double weatherTemperature) {
    this.weatherTemperature = weatherTemperature;
  }

  public Double getWeatherHumidity() {
    return weatherHumidity;
  }

  public void setWeatherHumidity(Double weatherHumidity) {
    this.weatherHumidity = weatherHumidity;
  }

  public Double getWeatherPrecipitation() {
    return weatherPrecipitation;
  }

  public void setWeatherPrecipitation(Double weatherPrecipitation) {
    this.weatherPrecipitation = weatherPrecipitation;
  }

  public Double getVaccinationCoveragePct() {
    return vaccinationCoveragePct;
  }

  public void setVaccinationCoveragePct(Double vaccinationCoveragePct) {
    this.vaccinationCoveragePct = vaccinationCoveragePct;
  }

  public String getRiskExplanation() {
    return riskExplanation;
  }

  public void setRiskExplanation(String riskExplanation) {
    this.riskExplanation = riskExplanation;
  }

  public String getRecommendedAction() {
    return recommendedAction;
  }

  public void setRecommendedAction(String recommendedAction) {
    this.recommendedAction = recommendedAction;
  }
}
