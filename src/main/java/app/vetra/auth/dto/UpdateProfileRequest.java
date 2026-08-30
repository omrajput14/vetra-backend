package app.vetra.auth.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

/** Request payload for updating active user profile information. */
public record UpdateProfileRequest(
    String fullName,
    String phone,
    String farmName,
    String village,
    String taluka,
    String district,
    String state,
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90.0 and 90.0")
        @DecimalMax(value = "90.0", message = "Latitude must be between -90.0 and 90.0")
        Double latitude,
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180.0 and 180.0")
        @DecimalMax(value = "180.0", message = "Longitude must be between -180.0 and 180.0")
        Double longitude,
    String clinicName,
    String clinicAddress,
    String specialization,
    String qualification,
    Integer yearsExperience,
    @com.fasterxml.jackson.annotation.JsonProperty("isAvailable")
        @com.fasterxml.jackson.annotation.JsonAlias({"available", "is_available"})
        Boolean isAvailable,
    @com.fasterxml.jackson.annotation.JsonProperty("emergencyAvailable")
        @com.fasterxml.jackson.annotation.JsonAlias({"isEmergencyAvailable", "emergency_available"})
        Boolean emergencyAvailable,
    String shiftSchedule,
    String profilePhotoUrl,
    String certificateUrl) {

  @SuppressWarnings("checkstyle:ParameterNumber")
  public UpdateProfileRequest(
      String fullName,
      String phone,
      String farmName,
      String village,
      String taluka,
      String district,
      String state,
      Double latitude,
      Double longitude,
      String clinicName,
      String specialization,
      String qualification,
      Integer yearsExperience) {
    this(
        fullName,
        phone,
        farmName,
        village,
        taluka,
        district,
        state,
        latitude,
        longitude,
        clinicName,
        null,
        specialization,
        qualification,
        yearsExperience,
        null,
        null,
        null,
        null,
        null);
  }
}
