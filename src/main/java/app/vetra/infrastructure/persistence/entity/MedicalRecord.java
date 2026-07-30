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
 * Entity representing an immutable Electronic Veterinary Medical Record (EVMR).
 * Medical records represent permanent clinical history associated with a completed appointment.
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
   * Plain-text prescription summary.
   * Note: Intentionally designed as text for initial stage, ready for future normalization
   * into structured prescription item entities (medicine name, dosage, frequency, duration).
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
}
