package app.vetra.disease.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when an outbreak cluster transitions from ACTIVE to MONITORING state.
 *
 * @param outbreakId outbreak UUID
 * @param diseaseName disease name
 * @param transitionedAt timestamp of transition
 */
public record OutbreakMonitoringEvent(
    UUID outbreakId,
    String diseaseName,
    Instant transitionedAt
) {}
