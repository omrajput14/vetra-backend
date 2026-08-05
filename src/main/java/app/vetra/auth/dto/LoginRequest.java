package app.vetra.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** DTO payload for user login (accepts email or phone as identifier). */
public record LoginRequest(@NotBlank String identifier, @NotBlank String password) {}
