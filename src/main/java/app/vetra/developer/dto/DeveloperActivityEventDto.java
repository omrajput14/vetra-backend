package app.vetra.developer.dto;

import java.time.Instant;

/** Unified real operational telemetry event item. */
public record DeveloperActivityEventDto(
    String id,
    String eventType,
    String actorRole,
    String actorIdentifier,
    String title,
    String description,
    String status,
    Instant timestamp,
    String entityId,
    String category) {}
