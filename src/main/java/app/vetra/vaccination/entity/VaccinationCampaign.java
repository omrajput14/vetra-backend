package app.vetra.vaccination.entity;

import app.vetra.disease.entity.Outbreak;
import app.vetra.infrastructure.exception.BusinessRuleException;
import app.vetra.infrastructure.persistence.entity.BaseEntity;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.vaccination.enums.CampaignPriority;
import app.vetra.vaccination.enums.CampaignStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing an official government vaccination campaign or emergency ring campaign.
 */
@Entity
@Table(name = "vaccination_campaigns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VaccinationCampaign extends BaseEntity {

  @Column(name = "campaign_name", nullable = false, length = 150)
  private String campaignName;

  @Column(name = "disease_name", nullable = false, length = 128)
  private String diseaseName;

  @Column(name = "target_district", nullable = false, length = 100)
  private String targetDistrict;

  @Column(name = "target_taluka", length = 100)
  private String targetTaluka;

  @Column(name = "target_livestock_count", nullable = false)
  @Builder.Default
  private int targetLivestockCount = 0;

  @Column(name = "planned_doses", nullable = false)
  private int plannedDoses;

  @Column(name = "administered_doses", nullable = false)
  @Builder.Default
  private int administeredDoses = 0;

  @Enumerated(EnumType.STRING)
  @Column(name = "priority", nullable = false, length = 30)
  @Builder.Default
  private CampaignPriority priority = CampaignPriority.MEDIUM;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  @Builder.Default
  private CampaignStatus status = CampaignStatus.PLANNED;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "outbreak_id")
  private Outbreak outbreak;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by_id", nullable = false)
  private User createdBy;

  @Column(name = "notes", columnDefinition = "TEXT")
  private String notes;

  /**
   * Transitions the campaign to a new status following strict lifecycle rules.
   *
   * @param targetStatus the new status
   * @throws BusinessRuleException if transition is not permitted
   */
  public void transitionTo(CampaignStatus targetStatus) {
    if (this.status == targetStatus) {
      return;
    }
    if (!this.status.canTransitionTo(targetStatus)) {
      throw new BusinessRuleException(
          String.format(
              "Invalid campaign status transition from %s to %s. Allowed next statuses: %s",
              this.status, targetStatus, this.status.validTransitions()),
          "CAMPAIGN_INVALID_TRANSITION");
    }
    this.status = targetStatus;
  }

  /**
   * Calculates the percentage of planned doses administered so far.
   *
   * @return percentage between 0.0 and 100.0
   */
  public double getCoverageProgressPercentage() {
    if (plannedDoses <= 0) {
      return 0.0;
    }
    return Math.min(100.0, ((double) administeredDoses / plannedDoses) * 100.0);
  }
}
