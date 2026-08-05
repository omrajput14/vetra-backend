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
    Integer animalCount) {}
