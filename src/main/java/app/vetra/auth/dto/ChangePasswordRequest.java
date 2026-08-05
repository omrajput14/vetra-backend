package app.vetra.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO payload for password update. */
public record ChangePasswordRequest(
    @NotBlank String currentPassword, @NotBlank @Size(min = 6, max = 100) String newPassword) {}
