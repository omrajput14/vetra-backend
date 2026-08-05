package app.vetra.auth.dto;

/** Standard authentication token response. */
public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    UserProfileDto user) {}
