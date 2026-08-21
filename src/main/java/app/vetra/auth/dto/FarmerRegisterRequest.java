package app.vetra.auth.dto;

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
    String district,
    String state,
    Double latitude,
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
      Integer animalCount) {
    this(
        email,
        phone,
        password,
        fullName,
        farmName,
        village,
        district,
        state,
        latitude,
        longitude,
        animalCount,
        "en");
  }
}
