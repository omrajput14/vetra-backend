package app.vetra.mortality.entity;

import app.vetra.infrastructure.persistence.entity.BaseEntity;
import app.vetra.infrastructure.persistence.entity.VetProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Referral entity routing an animal mortality report to a verified nearby veterinarian for clinical
 * review and confirmation.
 */
@Entity
@Table(
    name = "mortality_referrals",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_mortality_referral",
          columnNames = {"mortality_event_id", "veterinarian_id"})
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MortalityReferral extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "mortality_event_id", nullable = false)
  private AnimalMortalityEvent mortalityEvent;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "veterinarian_id", nullable = false)
  private VetProfile veterinarian;

  @Column(name = "status", nullable = false, length = 30)
  @Builder.Default
  private String status = "PENDING";

  @Column(name = "distance_km")
  private Double distanceKm;
}
