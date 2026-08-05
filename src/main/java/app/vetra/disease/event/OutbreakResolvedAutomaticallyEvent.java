package app.vetra.disease.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when an outbreak cluster is automatically resolved by the scheduler due to
 * inactivity.
 *
 * @param outbreakId outbreak UUID
 * @param diseaseName disease name
 * @param reason resolution reason
 * @param resolvedAt resolution timestamp
 */
public record OutbreakResolvedAutomaticallyEvent(
    UUID outbreakId, String diseaseName, String reason, Instant resolvedAt) {}
