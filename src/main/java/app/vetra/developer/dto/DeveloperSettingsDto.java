package app.vetra.developer.dto;

import java.util.List;

/**
 * Strict allowlist configuration payload for Developer Settings.
 * Contains ZERO secrets, signing keys, passwords, hashes, tokens, API keys, or DB credentials.
 */
public record DeveloperSettingsDto(
    String applicationName,
    String environment,
    String version,
    String databaseType,
    String databasePoolConfig,
    String jwtAlgorithm,
    long jwtAccessTokenTtlSeconds,
    long jwtRefreshTokenTtlSeconds,
    List<String> corsAllowedOrigins,
    boolean ragEnabled,
    String ragProvider,
    boolean aiGatewayEnabled,
    String aiDefaultProvider,
    String aiDefaultModel,
    String observabilityFramework) {}
