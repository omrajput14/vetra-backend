package app.vetra.auth.dto;

import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.infrastructure.persistence.enums.VerificationStatus;
import app.vetra.infrastructure.persistence.enums.VetMatchType;
import java.util.UUID;

/** Summary DTO for veterinarian directory and nearby vet selection. */
public record VetSummaryDto(
    UUID id,
    String name,
    String fullName,
    String registrationNumber,
    String qualification,
    String specialization,
    String clinic,
    String clinicName,
    String clinicAddress,
    String village,
    String taluka,
    String district,
    String state,
    Integer yearsExperience,
    @com.fasterxml.jackson.annotation.JsonProperty("isAvailable")
        Boolean isAvailable,
    @com.fasterxml.jackson.annotation.JsonProperty("emergencyAvailable")
        Boolean emergencyAvailable,
    Double rating,
    Double latitude,
    Double longitude,
    Double distanceKm,
    VetMatchType matchType,
    String phoneNumber,
    String phone,
    String email,
    VerificationStatus verificationStatus,
    Boolean verified,
    String shiftSchedule,
    String profilePhotoUrl,
    String certificateUrl,
    String certificateStatus) {

  @com.fasterxml.jackson.annotation.JsonProperty("available")
  public Boolean available() {
    return isAvailable;
  }

  @com.fasterxml.jackson.annotation.JsonProperty("isEmergencyAvailable")
  public Boolean isEmergencyAvailable() {
    return emergencyAvailable;
  }

  /** Converts a VetProfile entity to VetSummaryDto with default GENERAL match type and null distance. */
  public static VetSummaryDto fromEntity(VetProfile vet) {
    return fromEntity(vet, null, VetMatchType.GENERAL);
  }

  /** Converts a VetProfile entity to VetSummaryDto with explicit distance and match type. */
  public static VetSummaryDto fromEntity(
      VetProfile vet, Double distanceKm, VetMatchType matchType) {
    String phone = vet.getUser() != null ? vet.getUser().getPhone() : null;
    String name = vet.getFullName();
    String clinic = vet.getClinicName();
    VerificationStatus status =
        vet.getVerificationStatus() != null
            ? vet.getVerificationStatus()
            : VerificationStatus.PENDING;
    boolean isVerified = status == VerificationStatus.VERIFIED;

    return new VetSummaryDto(
        vet.getId(),
        name,
        name,
        vet.getRegistrationNumber(),
        vet.getQualification(),
        vet.getSpecialization(),
        clinic,
        clinic,
        vet.getClinicAddress(),
        vet.getVillage(),
        vet.getTaluka(),
        vet.getDistrict(),
        vet.getState(),
        vet.getYearsExperience(),
        vet.isAvailable(),
        vet.isEmergencyAvailable(),
        5.0,
        vet.getLatitude(),
        vet.getLongitude(),
        distanceKm,
        matchType != null ? matchType : VetMatchType.GENERAL,
        phone,
        phone,
        vet.getUser() != null ? vet.getUser().getEmail() : null,
        status,
        isVerified,
        vet.getShiftSchedule(),
        vet.getProfilePhotoUrl(),
        vet.getCertificateUrl(),
        vet.getCertificateStatus());
  }
}
