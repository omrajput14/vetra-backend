package app.vetra.disease.entity;

import app.vetra.infrastructure.persistence.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
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
}
