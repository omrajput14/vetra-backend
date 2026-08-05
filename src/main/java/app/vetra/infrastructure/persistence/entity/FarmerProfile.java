package app.vetra.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Profile information for livestock farmers. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "farmer_profiles")
public class FarmerProfile extends BaseEntity {

  @NotNull
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @NotNull
  @Column(name = "full_name", nullable = false)
  private String fullName;

  @Column(name = "farm_name")
  private String farmName;

  @Column(name = "village")
  private String village;

  @Column(name = "district")
  private String district;

  @Column(name = "state")
  private String state;

  @Column(name = "latitude")
  private Double latitude;

  @Column(name = "longitude")
  private Double longitude;

  @Column(name = "animal_count")
  private Integer animalCount;
}
