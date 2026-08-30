package app.vetra.infrastructure.persistence.entity;

import app.vetra.infrastructure.persistence.enums.VerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

  @Builder.Default
  @Column(name = "emergency_available", nullable = false)
  private boolean emergencyAvailable = true;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(name = "verification_status", nullable = false)
  private VerificationStatus verificationStatus = VerificationStatus.PENDING;

  @Column(name = "latitude")
  private Double latitude;

  @Column(name = "longitude")
  private Double longitude;

  @Column(name = "shift_schedule", columnDefinition = "TEXT")
  private String shiftSchedule;

  @Column(name = "profile_photo_url", columnDefinition = "TEXT")
  private String profilePhotoUrl;

  @Column(name = "certificate_url", columnDefinition = "TEXT")
  private String certificateUrl;

  @Column(name = "clinic_address")
  private String clinicAddress;

  @Column(name = "village")
  private String village;

  @Column(name = "taluka")
  private String taluka;

  @Column(name = "district")
  private String district;

  @Column(name = "state")
  private String state;

  @Builder.Default
  @Column(name = "certificate_status", nullable = false, length = 30)
  private String certificateStatus = "PENDING_VERIFICATION";

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

  public boolean isEmergencyAvailable() {
    return emergencyAvailable;
  }

  public void setEmergencyAvailable(boolean emergencyAvailable) {
    this.emergencyAvailable = emergencyAvailable;
  }

  public VerificationStatus getVerificationStatus() {
    return verificationStatus;
  }

  public void setVerificationStatus(VerificationStatus verificationStatus) {
    this.verificationStatus = verificationStatus;
  }

  public boolean isVerified() {
    return verificationStatus == VerificationStatus.VERIFIED;
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

  public String getShiftSchedule() {
    return shiftSchedule;
  }

  public void setShiftSchedule(String shiftSchedule) {
    this.shiftSchedule = shiftSchedule;
  }

  public String getProfilePhotoUrl() {
    return profilePhotoUrl;
  }

  public void setProfilePhotoUrl(String profilePhotoUrl) {
    this.profilePhotoUrl = profilePhotoUrl;
  }

  public String getCertificateUrl() {
    return certificateUrl;
  }

  public void setCertificateUrl(String certificateUrl) {
    this.certificateUrl = certificateUrl;
  }

  public String getClinicAddress() {
    return clinicAddress;
  }

  public void setClinicAddress(String clinicAddress) {
    this.clinicAddress = clinicAddress;
  }

  public String getCertificateStatus() {
    return certificateStatus;
  }

  public void setCertificateStatus(String certificateStatus) {
    this.certificateStatus = certificateStatus;
  }

  public String getVillage() {
    return village;
  }

  public void setVillage(String village) {
    this.village = village;
  }

  public String getTaluka() {
    return taluka;
  }

  public void setTaluka(String taluka) {
    this.taluka = taluka;
  }

  public String getDistrict() {
    return district;
  }

  public void setDistrict(String district) {
    this.district = district;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
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
    private boolean emergencyAvailable = true;
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;
    private Double latitude;
    private Double longitude;
    private String shiftSchedule;
    private String profilePhotoUrl;
    private String certificateUrl;
    private String clinicAddress;
    private String village;
    private String taluka;
    private String district;
    private String state;
    private String certificateStatus = "PENDING_VERIFICATION";

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

    public VetProfileBuilder emergencyAvailable(boolean emergencyAvailable) {
      this.emergencyAvailable = emergencyAvailable;
      return this;
    }

    public VetProfileBuilder verificationStatus(VerificationStatus verificationStatus) {
      this.verificationStatus = verificationStatus;
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

    public VetProfileBuilder shiftSchedule(String shiftSchedule) {
      this.shiftSchedule = shiftSchedule;
      return this;
    }

    public VetProfileBuilder profilePhotoUrl(String profilePhotoUrl) {
      this.profilePhotoUrl = profilePhotoUrl;
      return this;
    }

    public VetProfileBuilder certificateUrl(String certificateUrl) {
      this.certificateUrl = certificateUrl;
      return this;
    }

    public VetProfileBuilder clinicAddress(String clinicAddress) {
      this.clinicAddress = clinicAddress;
      return this;
    }

    public VetProfileBuilder village(String village) {
      this.village = village;
      return this;
    }

    public VetProfileBuilder taluka(String taluka) {
      this.taluka = taluka;
      return this;
    }

    public VetProfileBuilder district(String district) {
      this.district = district;
      return this;
    }

    public VetProfileBuilder state(String state) {
      this.state = state;
      return this;
    }

    public VetProfileBuilder certificateStatus(String certificateStatus) {
      this.certificateStatus = certificateStatus;
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
      profile.setEmergencyAvailable(this.emergencyAvailable);
      profile.setVerificationStatus(
          this.verificationStatus != null ? this.verificationStatus : VerificationStatus.PENDING);
      profile.setLatitude(this.latitude);
      profile.setLongitude(this.longitude);
      profile.setShiftSchedule(this.shiftSchedule);
      profile.setProfilePhotoUrl(this.profilePhotoUrl);
      profile.setCertificateUrl(this.certificateUrl);
      profile.setClinicAddress(this.clinicAddress);
      profile.setVillage(this.village);
      profile.setTaluka(this.taluka);
      profile.setDistrict(this.district);
      profile.setState(this.state);
      profile.setCertificateStatus(
          this.certificateStatus != null ? this.certificateStatus : "PENDING_VERIFICATION");
      return profile;
    }
  }
}
