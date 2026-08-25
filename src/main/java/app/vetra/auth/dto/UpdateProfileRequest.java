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
    String specialization,
    String qualification,
    Integer yearsExperience) {

  @SuppressWarnings("checkstyle:ParameterNumber")
  public UpdateProfileRequest(
      String fullName,
      String phone,
      String farmName,
      String village,
      String district,
      String state,
      String clinicName,
      String specialization,
      String qualification,
      Integer yearsExperience) {
    this(
        fullName,
        phone,
        farmName,
        village,
        null,
        district,
        state,
        null,
        null,
        clinicName,
        specialization,
        qualification,
        yearsExperience);
  }
}
