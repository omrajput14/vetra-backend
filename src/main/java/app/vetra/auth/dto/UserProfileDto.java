package app.vetra.auth.dto;

import app.vetra.infrastructure.persistence.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;

/** Unified user profile response DTO combining User and role profile details. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserProfileDto(
    UUID id,
    String email,
    String phone,
    UserRole role,
    boolean isActive,
    String preferredLanguage,
    String fullName,
    String farmName,
    String village,
    String taluka,
    String district,
    String state,
    Double latitude,
    Double longitude,
    Integer animalCount,
    String registrationNumber,
    String qualification,
    String specialization,
    String clinicName,
    Integer yearsExperience,
    @com.fasterxml.jackson.annotation.JsonProperty("isAvailable")
        Boolean isAvailable,
    @com.fasterxml.jackson.annotation.JsonProperty("emergencyAvailable")
        Boolean emergencyAvailable,
    String shiftSchedule,
    String profilePhotoUrl,
    String certificateUrl,
    String clinicAddress,
    String certificateStatus) {

  @com.fasterxml.jackson.annotation.JsonProperty("available")
  public Boolean available() {
    return isAvailable;
  }

  @com.fasterxml.jackson.annotation.JsonProperty("isEmergencyAvailable")
  public Boolean isEmergencyAvailable() {
    return emergencyAvailable;
  }

  @SuppressWarnings("checkstyle:ParameterNumber")
  public UserProfileDto(
      UUID id,
      String email,
      String phone,
      UserRole role,
      boolean isActive,
      String preferredLanguage,
      String fullName,
      String farmName,
      String village,
      String taluka,
      String district,
      String state,
      Double latitude,
      Double longitude,
      Integer animalCount,
      String registrationNumber,
      String qualification,
      String specialization,
      String clinicName,
      Integer yearsExperience,
      Boolean isAvailable,
      Boolean emergencyAvailable,
      String shiftSchedule) {
    this(
        id,
        email,
        phone,
        role,
        isActive,
        preferredLanguage,
        fullName,
        farmName,
        village,
        taluka,
        district,
        state,
        latitude,
        longitude,
        animalCount,
        registrationNumber,
        qualification,
        specialization,
        clinicName,
        yearsExperience,
        isAvailable,
        emergencyAvailable,
        shiftSchedule,
        null,
        null,
        null,
        null);
  }
}
