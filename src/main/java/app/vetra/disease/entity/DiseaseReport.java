package app.vetra.disease.entity;

import app.vetra.ai.entity.AIScan;
import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.entity.BaseEntity;
import app.vetra.infrastructure.persistence.entity.MedicalRecord;
import app.vetra.infrastructure.persistence.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing a disease surveillance report for an animal.
 */
@Entity
@Table(name = "disease_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiseaseReport extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "animal_id", nullable = false)
  private Animal animal;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "medical_record_id")
  private MedicalRecord medicalRecord;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ai_scan_id")
  private AIScan aiScan;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "reported_by", nullable = false)
  private User reportedBy;

  @Enumerated(EnumType.STRING)
  @Column(name = "report_source", nullable = false, length = 32)
  private DiseaseReportSource reportSource;

  @Enumerated(EnumType.STRING)
  @Column(name = "diagnosis_confidence_source", nullable = false, length = 32)
  @Builder.Default
  private DiagnosisConfidenceSource diagnosisConfidenceSource = DiagnosisConfidenceSource.VETERINARIAN;

  @Column(name = "disease_name", nullable = false, length = 128)
  private String diseaseName;

  @Enumerated(EnumType.STRING)
  @Column(name = "diagnosis_status", nullable = false, length = 32)
  private DiagnosisStatus diagnosisStatus;

  @Column(name = "latitude", nullable = false)
  private Double latitude;

  @Column(name = "longitude", nullable = false)
  private Double longitude;

  @Column(name = "notes", columnDefinition = "TEXT")
  private String notes;
}
