package app.vetra.disease.event;

import java.util.UUID;

/**
 * Event published when an active outbreak cluster is marked resolved.
 *
 * @param outbreakId outbreak UUID
 * @param diseaseName disease name
 */
public record OutbreakResolvedEvent(UUID outbreakId, String diseaseName) {}
