package app.vetra.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing an immutable Electronic Veterinary Medical Record (EVMR). Medical records
 * represent permanent clinical history associated with a completed appointment.
 */
@Entity
@Table(name = "medical_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY, optional = true)
  @JoinColumn(name = "appointment_id", nullable = true, unique = true)
  private Appointment appointment;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "animal_id", nullable = false)
  private Animal animal;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "farmer_id", nullable = false)
  private FarmerProfile farmer;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "veterinarian_id", nullable = false)
  private VetProfile veterinarian;

  @Column(name = "diagnosis", nullable = false, columnDefinition = "TEXT")
  private String diagnosis;

  @Column(name = "symptoms", columnDefinition = "TEXT")
  private String symptoms;

  @Column(name = "treatment", nullable = false, columnDefinition = "TEXT")
  private String treatment;

  /**
   * Plain-text prescription summary. Note: Intentionally designed as text for initial stage, ready
   * for future normalization into structured prescription item entities (medicine name, dosage,
   * frequency, duration).
   */
  @Column(name = "prescription", columnDefinition = "TEXT")
  private String prescription;

  @Column(name = "weight", precision = 6, scale = 2)
  private BigDecimal weight;

  @Column(name = "temperature", precision = 4, scale = 1)
  private BigDecimal temperature;

  @Column(name = "follow_up_date")
  private LocalDate followUpDate;

  @Column(name = "notes", columnDefinition = "TEXT")
  private String notes;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @Version
  @Column(name = "version", nullable = false)
  private Long version;

  @PrePersist
  protected void onCreate() {
    OffsetDateTime now = OffsetDateTime.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = OffsetDateTime.now();
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Appointment getAppointment() {
    return appointment;
  }

  public void setAppointment(Appointment appointment) {
    this.appointment = appointment;
  }

  public Animal getAnimal() {
    return animal;
  }

  public void setAnimal(Animal animal) {
    this.animal = animal;
  }

  public FarmerProfile getFarmer() {
    return farmer;
  }

  public void setFarmer(FarmerProfile farmer) {
    this.farmer = farmer;
  }

  public VetProfile getVeterinarian() {
    return veterinarian;
  }

  public void setVeterinarian(VetProfile veterinarian) {
    this.veterinarian = veterinarian;
  }

  public String getDiagnosis() {
    return diagnosis;
  }

  public void setDiagnosis(String diagnosis) {
    this.diagnosis = diagnosis;
  }

  public String getSymptoms() {
    return symptoms;
  }

  public void setSymptoms(String symptoms) {
    this.symptoms = symptoms;
  }

  public String getTreatment() {
    return treatment;
  }

  public void setTreatment(String treatment) {
    this.treatment = treatment;
  }

  public String getPrescription() {
    return prescription;
  }

  public void setPrescription(String prescription) {
    this.prescription = prescription;
  }

  public BigDecimal getWeight() {
    return weight;
  }

  public void setWeight(BigDecimal weight) {
    this.weight = weight;
  }

  public BigDecimal getTemperature() {
    return temperature;
  }

  public void setTemperature(BigDecimal temperature) {
    this.temperature = temperature;
  }

  public LocalDate getFollowUpDate() {
    return followUpDate;
  }

  public void setFollowUpDate(LocalDate followUpDate) {
    this.followUpDate = followUpDate;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  public static MedicalRecordBuilder builder() {
    return new MedicalRecordBuilder();
  }

  public static class MedicalRecordBuilder {
    private UUID id;
    private Appointment appointment;
    private Animal animal;
    private FarmerProfile farmer;
    private VetProfile veterinarian;
    private String diagnosis;
    private String symptoms;
    private String treatment;
    private String prescription;
    private BigDecimal weight;
    private BigDecimal temperature;
    private LocalDate followUpDate;
    private String notes;

    public MedicalRecordBuilder id(UUID id) {
      this.id = id;
      return this;
    }

    public MedicalRecordBuilder appointment(Appointment appointment) {
      this.appointment = appointment;
      return this;
    }

    public MedicalRecordBuilder animal(Animal animal) {
      this.animal = animal;
      return this;
    }

    public MedicalRecordBuilder farmer(FarmerProfile farmer) {
      this.farmer = farmer;
      return this;
    }

    public MedicalRecordBuilder veterinarian(VetProfile veterinarian) {
      this.veterinarian = veterinarian;
      return this;
    }

    public MedicalRecordBuilder diagnosis(String diagnosis) {
      this.diagnosis = diagnosis;
      return this;
    }

    public MedicalRecordBuilder symptoms(String symptoms) {
      this.symptoms = symptoms;
      return this;
    }

    public MedicalRecordBuilder treatment(String treatment) {
      this.treatment = treatment;
      return this;
    }

    public MedicalRecordBuilder prescription(String prescription) {
      this.prescription = prescription;
      return this;
    }

    public MedicalRecordBuilder weight(BigDecimal weight) {
      this.weight = weight;
      return this;
    }

    public MedicalRecordBuilder temperature(BigDecimal temperature) {
      this.temperature = temperature;
      return this;
    }

    public MedicalRecordBuilder followUpDate(LocalDate followUpDate) {
      this.followUpDate = followUpDate;
      return this;
    }

    public MedicalRecordBuilder notes(String notes) {
      this.notes = notes;
      return this;
    }

    public MedicalRecord build() {
      MedicalRecord record = new MedicalRecord();
      record.setId(this.id);
      record.setAppointment(this.appointment);
      record.setAnimal(this.animal);
      record.setFarmer(this.farmer);
      record.setVeterinarian(this.veterinarian);
      record.setDiagnosis(this.diagnosis);
      record.setSymptoms(this.symptoms);
      record.setTreatment(this.treatment);
      record.setPrescription(this.prescription);
      record.setWeight(this.weight);
      record.setTemperature(this.temperature);
      record.setFollowUpDate(this.followUpDate);
      record.setNotes(this.notes);
      return record;
    }
  }
}
