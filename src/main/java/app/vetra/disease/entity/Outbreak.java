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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Entity representing a disease outbreak cluster under active surveillance. */
@Entity
@Table(name = "outbreaks")
@Getter
@Setter
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

  public static OutbreakBuilder builder() {
    return new OutbreakBuilder();
  }

  public static class OutbreakBuilder {
    private String diseaseName;
    private String severity = "MEDIUM";
    private OutbreakStatus status = OutbreakStatus.ACTIVE;
    private OutbreakRiskScore riskScore = OutbreakRiskScore.MEDIUM;
    private OutbreakTrend trend = OutbreakTrend.STABLE;
    private Double centerLatitude;
    private Double centerLongitude;
    private Double radiusKm = 10.0;
    private Integer affectedReportsCount = 0;
    private Integer evaluationWindowHours = 72;
    private Instant lastCaseReportedAt = Instant.now();
    private Instant lastEvaluatedAt = Instant.now();
    private Instant resolvedAt;
    private String resolutionReason;

    public OutbreakBuilder diseaseName(String diseaseName) {
      this.diseaseName = diseaseName;
      return this;
    }

    public OutbreakBuilder severity(String severity) {
      this.severity = severity;
      return this;
    }

    public OutbreakBuilder status(OutbreakStatus status) {
      this.status = status;
      return this;
    }

    public OutbreakBuilder riskScore(OutbreakRiskScore riskScore) {
      this.riskScore = riskScore;
      return this;
    }

    public OutbreakBuilder trend(OutbreakTrend trend) {
      this.trend = trend;
      return this;
    }

    public OutbreakBuilder centerLatitude(Double centerLatitude) {
      this.centerLatitude = centerLatitude;
      return this;
    }

    public OutbreakBuilder centerLongitude(Double centerLongitude) {
      this.centerLongitude = centerLongitude;
      return this;
    }

    public OutbreakBuilder radiusKm(Double radiusKm) {
      this.radiusKm = radiusKm;
      return this;
    }

    public OutbreakBuilder affectedReportsCount(Integer affectedReportsCount) {
      this.affectedReportsCount = affectedReportsCount;
      return this;
    }

    public OutbreakBuilder evaluationWindowHours(Integer evaluationWindowHours) {
      this.evaluationWindowHours = evaluationWindowHours;
      return this;
    }

    public OutbreakBuilder lastCaseReportedAt(Instant lastCaseReportedAt) {
      this.lastCaseReportedAt = lastCaseReportedAt;
      return this;
    }

    public OutbreakBuilder lastEvaluatedAt(Instant lastEvaluatedAt) {
      this.lastEvaluatedAt = lastEvaluatedAt;
      return this;
    }

    public OutbreakBuilder resolvedAt(Instant resolvedAt) {
      this.resolvedAt = resolvedAt;
      return this;
    }

    public OutbreakBuilder resolutionReason(String resolutionReason) {
      this.resolutionReason = resolutionReason;
      return this;
    }

    public Outbreak build() {
      Outbreak outbreak = new Outbreak();
      outbreak.setDiseaseName(this.diseaseName);
      outbreak.setSeverity(this.severity);
      outbreak.setStatus(this.status);
      outbreak.setRiskScore(this.riskScore);
      outbreak.setTrend(this.trend);
      outbreak.setCenterLatitude(this.centerLatitude);
      outbreak.setCenterLongitude(this.centerLongitude);
      outbreak.setRadiusKm(this.radiusKm);
      outbreak.setAffectedReportsCount(this.affectedReportsCount);
      outbreak.setEvaluationWindowHours(this.evaluationWindowHours);
      outbreak.setLastCaseReportedAt(this.lastCaseReportedAt);
      outbreak.setLastEvaluatedAt(this.lastEvaluatedAt);
      outbreak.setResolvedAt(this.resolvedAt);
      outbreak.setResolutionReason(this.resolutionReason);
      return outbreak;
    }
  }
}
