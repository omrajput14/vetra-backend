package app.vetra.mortality.entity;

import app.vetra.infrastructure.persistence.entity.BaseEntity;
import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.mortality.enums.MortalityCauseCategory;
import app.vetra.mortality.enums.MortalityReportStatus;
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
 * Immutable audit log record capturing veterinarian verification, review, and status changes
 * on livestock mortality reports.
 */
@Entity
@Table(name = "mortality_review_audits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MortalityReviewAudit extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "mortality_event_id", nullable = false)
  private AnimalMortalityEvent mortalityEvent;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "veterinarian_id", nullable = false)
  private VetProfile veterinarian;

  @Column(name = "action", nullable = false, length = 50)
  private String action;

  @Enumerated(EnumType.STRING)
  @Column(name = "old_status", nullable = false, length = 50)
  private MortalityReportStatus oldStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "new_status", nullable = false, length = 50)
  private MortalityReportStatus newStatus;

  @Column(name = "clinical_notes", columnDefinition = "TEXT")
  private String clinicalNotes;

  @Enumerated(EnumType.STRING)
  @Column(name = "confirmed_cause_category", length = 50)
  private MortalityCauseCategory confirmedCauseCategory;

  @Column(name = "confirmed_disease_name", length = 128)
  private String confirmedDiseaseName;
}
