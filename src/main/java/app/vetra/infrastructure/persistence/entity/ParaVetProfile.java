package app.vetra.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Field para-vet (livestock supervisor): first-line check of farmers' AI scans. Can escalate a
 * scan to a vet or close it, but cannot confirm a diagnosis (only registered vets can).
 */
@Entity
@Table(name = "para_vet_profiles")
@Getter
@Setter
@NoArgsConstructor
public class ParaVetProfile extends BaseEntity {

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Column(name = "full_name", nullable = false)
  private String fullName;

  @Column(name = "district", length = 100)
  private String district;

  @Column(name = "taluka", length = 100)
  private String taluka;

  @Column(name = "latitude")
  private Double latitude;

  @Column(name = "longitude")
  private Double longitude;
}
