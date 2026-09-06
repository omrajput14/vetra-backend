package app.vetra.mortality.entity;

import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.entity.BaseEntity;
import app.vetra.infrastructure.persistence.entity.FarmerProfile;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.mortality.enums.MortalityCauseCategory;
import app.vetra.mortality.enums.MortalityReportStatus;
import app.vetra.mortality.enums.MortalitySource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Entity representing a recorded livestock animal mortality event. */
@Entity
@Table(name = "animal_mortality_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnimalMortalityEvent extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "animal_id", nullable = false)
  private Animal animal;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "farmer_id", nullable = false)
  private FarmerProfile farmer;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "reported_by", nullable = false)
  private User reportedBy;

  @Column(name = "tag_number", nullable = false, length = 100)
  private String tagNumber;

  @Column(name = "qr_code_id", length = 100)
  private String qrCodeId;

  @Enumerated(EnumType.STRING)
  @Column(name = "cause_category", nullable = false, length = 50)
  private MortalityCauseCategory causeCategory;

  @Column(name = "cause_description", columnDefinition = "TEXT")
  private String causeDescription;

  @Column(name = "disease_name", length = 128)
  private String diseaseName;

  @Column(name = "recently_treated", nullable = false)
  @Builder.Default
  private boolean recentlyTreated = false;

  @Column(name = "treatment_notes", columnDefinition = "TEXT")
  private String treatmentNotes;

  @Column(name = "notes", columnDefinition = "TEXT")
  private String notes;

  @Enumerated(EnumType.STRING)
  @Column(name = "source", nullable = false, length = 30)
  @Builder.Default
  private MortalitySource source = MortalitySource.FARMER_REPORTED;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  @Builder.Default
  private MortalityReportStatus status = MortalityReportStatus.REPORTED;

  @Column(name = "death_date_time")
  private Instant deathDateTime;

  @Column(name = "reported_at", nullable = false)
  @Builder.Default
  private Instant reportedAt = Instant.now();

  @Column(name = "latitude")
  private Double latitude;

  @Column(name = "longitude")
  private Double longitude;

  @Column(name = "location_accuracy")
  private Double locationAccuracy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vet_reviewed_by")
  private app.vetra.infrastructure.persistence.entity.VetProfile vetReviewedBy;

  @Column(name = "vet_reviewed_at")
  private Instant vetReviewedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "vet_cause_category", length = 50)
  private MortalityCauseCategory vetCauseCategory;

  @Column(name = "vet_disease_name", length = 128)
  private String vetDiseaseName;

  @Column(name = "vet_clinical_notes", columnDefinition = "TEXT")
  private String vetClinicalNotes;

  @Column(name = "vet_rejection_reason", columnDefinition = "TEXT")
  private String vetRejectionReason;

  @Column(name = "post_mortem_conducted", nullable = false)
  @Builder.Default
  private boolean postMortemConducted = false;
}
