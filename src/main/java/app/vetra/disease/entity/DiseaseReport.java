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

/** Entity representing a disease surveillance report for an animal. */
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
  private DiagnosisConfidenceSource diagnosisConfidenceSource =
      DiagnosisConfidenceSource.VETERINARIAN;

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

  public Animal getAnimal() {
    return animal;
  }

  public void setAnimal(Animal animal) {
    this.animal = animal;
  }

  public MedicalRecord getMedicalRecord() {
    return medicalRecord;
  }

  public void setMedicalRecord(MedicalRecord medicalRecord) {
    this.medicalRecord = medicalRecord;
  }

  public AIScan getAiScan() {
    return aiScan;
  }

  public void setAiScan(AIScan aiScan) {
    this.aiScan = aiScan;
  }

  public User getReportedBy() {
    return reportedBy;
  }

  public void setReportedBy(User reportedBy) {
    this.reportedBy = reportedBy;
  }

  public DiseaseReportSource getReportSource() {
    return reportSource;
  }

  public void setReportSource(DiseaseReportSource reportSource) {
    this.reportSource = reportSource;
  }

  public DiagnosisConfidenceSource getDiagnosisConfidenceSource() {
    return diagnosisConfidenceSource;
  }

  public void setDiagnosisConfidenceSource(DiagnosisConfidenceSource diagnosisConfidenceSource) {
    this.diagnosisConfidenceSource = diagnosisConfidenceSource;
  }

  public String getDiseaseName() {
    return diseaseName;
  }

  public void setDiseaseName(String diseaseName) {
    this.diseaseName = diseaseName;
  }

  public DiagnosisStatus getDiagnosisStatus() {
    return diagnosisStatus;
  }

  public void setDiagnosisStatus(DiagnosisStatus diagnosisStatus) {
    this.diagnosisStatus = diagnosisStatus;
  }

  public Double getLatitude() {
    return latitude;
  }

  public void setLatitude(Double latitude) {
    this.latitude = latitude;
  }

  public Double getLongitude() {
    return longitude;
  }

  public void setLongitude(Double longitude) {
    this.longitude = longitude;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public static DiseaseReportBuilder builder() {
    return new DiseaseReportBuilder();
  }

  public static class DiseaseReportBuilder {
    private Animal animal;
    private MedicalRecord medicalRecord;
    private AIScan aiScan;
    private User reportedBy;
    private DiseaseReportSource reportSource;
    private DiagnosisConfidenceSource diagnosisConfidenceSource =
        DiagnosisConfidenceSource.VETERINARIAN;
    private String diseaseName;
    private DiagnosisStatus diagnosisStatus;
    private Double latitude;
    private Double longitude;
    private String notes;

    public DiseaseReportBuilder animal(Animal animal) {
      this.animal = animal;
      return this;
    }

    public DiseaseReportBuilder medicalRecord(MedicalRecord medicalRecord) {
      this.medicalRecord = medicalRecord;
      return this;
    }

    public DiseaseReportBuilder aiScan(AIScan aiScan) {
      this.aiScan = aiScan;
      return this;
    }

    public DiseaseReportBuilder reportedBy(User reportedBy) {
      this.reportedBy = reportedBy;
      return this;
    }

    public DiseaseReportBuilder reportSource(DiseaseReportSource reportSource) {
      this.reportSource = reportSource;
      return this;
    }

    public DiseaseReportBuilder diagnosisConfidenceSource(
        DiagnosisConfidenceSource diagnosisConfidenceSource) {
      this.diagnosisConfidenceSource = diagnosisConfidenceSource;
      return this;
    }

    public DiseaseReportBuilder diseaseName(String diseaseName) {
      this.diseaseName = diseaseName;
      return this;
    }

    public DiseaseReportBuilder diagnosisStatus(DiagnosisStatus diagnosisStatus) {
      this.diagnosisStatus = diagnosisStatus;
      return this;
    }

    public DiseaseReportBuilder latitude(Double latitude) {
      this.latitude = latitude;
      return this;
    }

    public DiseaseReportBuilder longitude(Double longitude) {
      this.longitude = longitude;
      return this;
    }

    public DiseaseReportBuilder notes(String notes) {
      this.notes = notes;
      return this;
    }

    public DiseaseReport build() {
      DiseaseReport report = new DiseaseReport();
      report.setAnimal(this.animal);
      report.setMedicalRecord(this.medicalRecord);
      report.setAiScan(this.aiScan);
      report.setReportedBy(this.reportedBy);
      report.setReportSource(this.reportSource);
      report.setDiagnosisConfidenceSource(this.diagnosisConfidenceSource);
      report.setDiseaseName(this.diseaseName);
      report.setDiagnosisStatus(this.diagnosisStatus);
      report.setLatitude(this.latitude);
      report.setLongitude(this.longitude);
      report.setNotes(this.notes);
      return report;
    }
  }
}
