package app.vetra.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** DTO payload for refreshing access token. */
public record RefreshTokenRequest(@NotBlank String refreshToken) {}
