package app.vetra.disease.event;

import java.time.Instant;

/**
 * Event published when spatial disease heatmap hotspot calculations are refreshed.
 *
 * @param totalHotspots total active hotspot centroids
 * @param timestamp refresh timestamp
 */
public record HeatmapUpdatedEvent(int totalHotspots, Instant timestamp) {}
