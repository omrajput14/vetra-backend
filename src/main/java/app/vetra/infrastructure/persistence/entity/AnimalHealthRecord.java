package app.vetra.infrastructure.persistence.entity;

import app.vetra.infrastructure.persistence.enums.HealthRecordSource;
import app.vetra.infrastructure.persistence.enums.HealthRecordType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Animal lifetime medical history and health timeline event entity. */
@Entity
@Table(name = "animal_health_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnimalHealthRecord extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "animal_id", nullable = false)
  private Animal animal;

  @Enumerated(EnumType.STRING)
  @Column(name = "record_type", nullable = false, length = 30)
  private HealthRecordType recordType;

  @Enumerated(EnumType.STRING)
  @Column(name = "source", nullable = false, length = 30)
  private HealthRecordSource source;

  @Column(name = "title", nullable = false, length = 200)
  private String title;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "symptoms", columnDefinition = "TEXT")
  private String symptoms;

  @Column(name = "diagnosis", columnDefinition = "TEXT")
  private String diagnosis;

  @Column(name = "treatment", columnDefinition = "TEXT")
  private String treatment;

  @Column(name = "veterinarian_id")
  private UUID veterinarianId;

  @Column(name = "veterinarian_name", length = 150)
  private String veterinarianName;

  @Column(name = "document_url", columnDefinition = "TEXT")
  private String documentUrl;

  @Column(name = "vaccine_name", length = 150)
  private String vaccineName;

  @Column(name = "next_due_date")
  private java.time.LocalDate nextDueDate;

  @Column(name = "batch_number", length = 100)
  private String batchNumber;

  @Column(name = "recorded_at", nullable = false)
  private LocalDateTime recordedAt;

  public Animal getAnimal() {
    return animal;
  }

  public void setAnimal(Animal animal) {
    this.animal = animal;
  }

  public HealthRecordType getRecordType() {
    return recordType;
  }

  public void setRecordType(HealthRecordType recordType) {
    this.recordType = recordType;
  }

  public HealthRecordSource getSource() {
    return source;
  }

  public void setSource(HealthRecordSource source) {
    this.source = source;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getSymptoms() {
    return symptoms;
  }

  public void setSymptoms(String symptoms) {
    this.symptoms = symptoms;
  }

  public String getDiagnosis() {
    return diagnosis;
  }

  public void setDiagnosis(String diagnosis) {
    this.diagnosis = diagnosis;
  }

  public String getTreatment() {
    return treatment;
  }

  public void setTreatment(String treatment) {
    this.treatment = treatment;
  }

  public UUID getVeterinarianId() {
    return veterinarianId;
  }

  public void setVeterinarianId(UUID veterinarianId) {
    this.veterinarianId = veterinarianId;
  }

  public String getVeterinarianName() {
    return veterinarianName;
  }

  public void setVeterinarianName(String veterinarianName) {
    this.veterinarianName = veterinarianName;
  }

  public LocalDateTime getRecordedAt() {
    return recordedAt;
  }

  public void setRecordedAt(LocalDateTime recordedAt) {
    this.recordedAt = recordedAt;
  }

  public String getDocumentUrl() {
    return documentUrl;
  }

  public void setDocumentUrl(String documentUrl) {
    this.documentUrl = documentUrl;
  }

  public String getVaccineName() {
    return vaccineName;
  }

  public void setVaccineName(String vaccineName) {
    this.vaccineName = vaccineName;
  }

  public java.time.LocalDate getNextDueDate() {
    return nextDueDate;
  }

  public void setNextDueDate(java.time.LocalDate nextDueDate) {
    this.nextDueDate = nextDueDate;
  }

  public String getBatchNumber() {
    return batchNumber;
  }

  public void setBatchNumber(String batchNumber) {
    this.batchNumber = batchNumber;
  }

  public static AnimalHealthRecordBuilder builder() {
    return new AnimalHealthRecordBuilder();
  }

  public static class AnimalHealthRecordBuilder {
    private Animal animal;
    private HealthRecordType recordType;
    private HealthRecordSource source;
    private String title;
    private String description;
    private String symptoms;
    private String diagnosis;
    private String treatment;
    private UUID veterinarianId;
    private String veterinarianName;
    private String documentUrl;
    private String vaccineName;
    private java.time.LocalDate nextDueDate;
    private String batchNumber;
    private LocalDateTime recordedAt;

    public AnimalHealthRecordBuilder animal(Animal animal) {
      this.animal = animal;
      return this;
    }

    public AnimalHealthRecordBuilder recordType(HealthRecordType recordType) {
      this.recordType = recordType;
      return this;
    }

    public AnimalHealthRecordBuilder source(HealthRecordSource source) {
      this.source = source;
      return this;
    }

    public AnimalHealthRecordBuilder title(String title) {
      this.title = title;
      return this;
    }

    public AnimalHealthRecordBuilder description(String description) {
      this.description = description;
      return this;
    }

    public AnimalHealthRecordBuilder symptoms(String symptoms) {
      this.symptoms = symptoms;
      return this;
    }

    public AnimalHealthRecordBuilder diagnosis(String diagnosis) {
      this.diagnosis = diagnosis;
      return this;
    }

    public AnimalHealthRecordBuilder treatment(String treatment) {
      this.treatment = treatment;
      return this;
    }

    public AnimalHealthRecordBuilder veterinarianId(UUID veterinarianId) {
      this.veterinarianId = veterinarianId;
      return this;
    }

    public AnimalHealthRecordBuilder veterinarianName(String veterinarianName) {
      this.veterinarianName = veterinarianName;
      return this;
    }

    public AnimalHealthRecordBuilder documentUrl(String documentUrl) {
      this.documentUrl = documentUrl;
      return this;
    }

    public AnimalHealthRecordBuilder vaccineName(String vaccineName) {
      this.vaccineName = vaccineName;
      return this;
    }

    public AnimalHealthRecordBuilder nextDueDate(java.time.LocalDate nextDueDate) {
      this.nextDueDate = nextDueDate;
      return this;
    }

    public AnimalHealthRecordBuilder batchNumber(String batchNumber) {
      this.batchNumber = batchNumber;
      return this;
    }

    public AnimalHealthRecordBuilder recordedAt(LocalDateTime recordedAt) {
      this.recordedAt = recordedAt;
      return this;
    }

    public AnimalHealthRecord build() {
      AnimalHealthRecord record = new AnimalHealthRecord();
      record.setAnimal(this.animal);
      record.setRecordType(this.recordType != null ? this.recordType : HealthRecordType.OBSERVATION);
      record.setSource(this.source != null ? this.source : HealthRecordSource.SYSTEM);
      record.setTitle(this.title);
      record.setDescription(this.description);
      record.setSymptoms(this.symptoms);
      record.setDiagnosis(this.diagnosis);
      record.setTreatment(this.treatment);
      record.setVeterinarianId(this.veterinarianId);
      record.setVeterinarianName(this.veterinarianName);
      record.setDocumentUrl(this.documentUrl);
      record.setVaccineName(this.vaccineName);
      record.setNextDueDate(this.nextDueDate);
      record.setBatchNumber(this.batchNumber);
      record.setRecordedAt(this.recordedAt != null ? this.recordedAt : LocalDateTime.now());
      return record;
    }
  }
}
