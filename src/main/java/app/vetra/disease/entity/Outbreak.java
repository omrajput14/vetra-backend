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

/**
 * Entity representing a disease outbreak cluster under active surveillance.
 */
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
}
