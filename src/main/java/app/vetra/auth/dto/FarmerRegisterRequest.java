package app.vetra.auth.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO payload for farmer account registration. */
public record FarmerRegisterRequest(
    @NotBlank @Email String email,
    String phone,
    @NotBlank @Size(min = 6, max = 100) String password,
    @NotBlank String fullName,
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
    Integer animalCount,
    String preferredLanguage) {

  @SuppressWarnings("checkstyle:ParameterNumber")
  public FarmerRegisterRequest(
      String email,
      String phone,
      String password,
      String fullName,
      String farmName,
      String village,
      String district,
      String state,
      Double latitude,
      Double longitude,
      Integer animalCount,
      String preferredLanguage) {
    this(
        email,
        phone,
        password,
        fullName,
        farmName,
        village,
        null,
        district,
        state,
        latitude,
        longitude,
        animalCount,
        preferredLanguage);
  }

  @SuppressWarnings("checkstyle:ParameterNumber")
  public FarmerRegisterRequest(
      String email,
      String phone,
      String password,
      String fullName,
      String farmName,
      String village,
      String district,
      String state,
      Double latitude,
      Double longitude,
      Integer animalCount) {
    this(
        email,
        phone,
        password,
        fullName,
        farmName,
        village,
        null,
        district,
        state,
        latitude,
        longitude,
        animalCount,
        "en");
  }
}
