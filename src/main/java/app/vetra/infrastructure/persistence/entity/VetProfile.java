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

/** Profile information for licensed veterinarians. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "vet_profiles")
public class VetProfile extends BaseEntity {

  @NotNull
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @NotNull
  @Column(name = "full_name", nullable = false)
  private String fullName;

  @NotNull
  @Column(name = "registration_number", nullable = false, unique = true)
  private String registrationNumber;

  @Column(name = "qualification")
  private String qualification;

  @Column(name = "specialization")
  private String specialization;

  @Column(name = "clinic_name")
  private String clinicName;

  @Column(name = "years_experience")
  private Integer yearsExperience;

  @Builder.Default
  @Column(name = "is_available", nullable = false)
  private boolean isAvailable = true;

  @Column(name = "latitude")
  private Double latitude;

  @Column(name = "longitude")
  private Double longitude;
}
