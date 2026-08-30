package app.vetra.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO payload for veterinarian account registration. */
public record VetRegisterRequest(
    @NotBlank @Email String email,
    String phone,
    @NotBlank @Size(min = 6, max = 100) String password,
    @NotBlank String fullName,
    @NotBlank String registrationNumber,
    String qualification,
    String specialization,
    String clinicName,
    String clinicAddress,
    String village,
    String taluka,
    String district,
    String state,
    Integer yearsExperience,
    Double latitude,
    Double longitude,
    String preferredLanguage) {

  @SuppressWarnings("checkstyle:ParameterNumber")
  public VetRegisterRequest(
      String email,
      String phone,
      String password,
      String fullName,
      String registrationNumber,
      String qualification,
      String specialization,
      String clinicName,
      Integer yearsExperience,
      Double latitude,
      Double longitude) {
    this(
        email,
        phone,
        password,
        fullName,
        registrationNumber,
        qualification,
        specialization,
        clinicName,
        null,
        null,
        null,
        null,
        null,
        yearsExperience,
        latitude,
        longitude,
        "en");
  }

  @SuppressWarnings("checkstyle:ParameterNumber")
  public VetRegisterRequest(
      String email,
      String phone,
      String password,
      String fullName,
      String registrationNumber,
      String qualification,
      String specialization,
      String clinicName,
      Integer yearsExperience,
      Double latitude,
      Double longitude,
      String preferredLanguage) {
    this(
        email,
        phone,
        password,
        fullName,
        registrationNumber,
        qualification,
        specialization,
        clinicName,
        null,
        null,
        null,
        null,
        null,
        yearsExperience,
        latitude,
        longitude,
        preferredLanguage);
  }
}
