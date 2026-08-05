package app.vetra.disease.event;

import app.vetra.disease.entity.OutbreakTrend;
import java.util.UUID;

/**
 * Event published when an outbreak cluster's velocity trend changes (e.g. STABLE -> INCREASING).
 *
 * @param outbreakId outbreak UUID
 * @param diseaseName disease name
 * @param oldTrend previous trend
 * @param newTrend updated trend
 */
public record OutbreakTrendChangedEvent(
    UUID outbreakId, String diseaseName, OutbreakTrend oldTrend, OutbreakTrend newTrend) {}
