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

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getRegistrationNumber() {
    return registrationNumber;
  }

  public void setRegistrationNumber(String registrationNumber) {
    this.registrationNumber = registrationNumber;
  }

  public String getQualification() {
    return qualification;
  }

  public void setQualification(String qualification) {
    this.qualification = qualification;
  }

  public String getSpecialization() {
    return specialization;
  }

  public void setSpecialization(String specialization) {
    this.specialization = specialization;
  }

  public String getClinicName() {
    return clinicName;
  }

  public void setClinicName(String clinicName) {
    this.clinicName = clinicName;
  }

  public Integer getYearsExperience() {
    return yearsExperience;
  }

  public void setYearsExperience(Integer yearsExperience) {
    this.yearsExperience = yearsExperience;
  }

  public boolean isAvailable() {
    return isAvailable;
  }

  public void setAvailable(boolean available) {
    isAvailable = available;
  }

  public Double getLatitude() {
    return latitude;
  }

  public void setLatitude(Double latitude) {
    this.latitude = latitude;
  }

  public Double getLongitude() {
    return longitude;
  }

  public void setLongitude(Double longitude) {
    this.longitude = longitude;
  }

  public static VetProfileBuilder builder() {
    return new VetProfileBuilder();
  }

  public static class VetProfileBuilder {
    private User user;
    private String fullName;
    private String registrationNumber;
    private String qualification;
    private String specialization;
    private String clinicName;
    private Integer yearsExperience;
    private boolean isAvailable = true;
    private Double latitude;
    private Double longitude;

    public VetProfileBuilder user(User user) {
      this.user = user;
      return this;
    }

    public VetProfileBuilder fullName(String fullName) {
      this.fullName = fullName;
      return this;
    }

    public VetProfileBuilder registrationNumber(String registrationNumber) {
      this.registrationNumber = registrationNumber;
      return this;
    }

    public VetProfileBuilder qualification(String qualification) {
      this.qualification = qualification;
      return this;
    }

    public VetProfileBuilder specialization(String specialization) {
      this.specialization = specialization;
      return this;
    }

    public VetProfileBuilder clinicName(String clinicName) {
      this.clinicName = clinicName;
      return this;
    }

    public VetProfileBuilder yearsExperience(Integer yearsExperience) {
      this.yearsExperience = yearsExperience;
      return this;
    }

    public VetProfileBuilder isAvailable(boolean isAvailable) {
      this.isAvailable = isAvailable;
      return this;
    }

    public VetProfileBuilder latitude(Double latitude) {
      this.latitude = latitude;
      return this;
    }

    public VetProfileBuilder longitude(Double longitude) {
      this.longitude = longitude;
      return this;
    }

    public VetProfile build() {
      VetProfile profile = new VetProfile();
      profile.setUser(this.user);
      profile.setFullName(this.fullName);
      profile.setRegistrationNumber(this.registrationNumber);
      profile.setQualification(this.qualification);
      profile.setSpecialization(this.specialization);
      profile.setClinicName(this.clinicName);
      profile.setYearsExperience(this.yearsExperience);
      profile.setAvailable(this.isAvailable);
      profile.setLatitude(this.latitude);
      profile.setLongitude(this.longitude);
      return profile;
    }
  }
}
