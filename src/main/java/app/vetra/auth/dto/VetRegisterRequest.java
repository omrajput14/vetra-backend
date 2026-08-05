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
    Integer yearsExperience,
    Double latitude,
    Double longitude) {}
