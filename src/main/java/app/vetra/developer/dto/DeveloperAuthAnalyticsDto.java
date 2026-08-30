package app.vetra.developer.dto;

import app.vetra.infrastructure.persistence.enums.UserRole;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Authentication telemetry and safe session metadata payload. */
public record DeveloperAuthAnalyticsDto(
    long totalLoginAttempts,
    long successfulLogins,
    long failedLogins,
    double overallSuccessRatePercent,
    long farmerLoginSuccess,
    long farmerLoginFailure,
    long vetLoginSuccess,
    long vetLoginFailure,
    long activeSessionsCount,
    long revokedSessionsCount,
    long totalTokensIssued,
    List<SafeSessionMetadataDto> recentActiveSessions,
    Map<String, String> telemetryNotes) {

  /** Non-sensitive active session record without any token strings or hashes. */
  public record SafeSessionMetadataDto(
      UUID sessionId,
      UUID userId,
      String userIdentifier,
      UserRole role,
      Instant createdAt,
      Instant expiryDate,
      boolean isRevoked,
      long sessionAgeSeconds,
      boolean isExpired) {}
}
