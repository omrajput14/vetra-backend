package app.vetra.disease.event;

import app.vetra.disease.entity.OutbreakRiskScore;
import java.util.UUID;

/**
 * Event published when an outbreak cluster's risk severity score changes.
 *
 * @param outbreakId outbreak UUID
 * @param diseaseName disease name
 * @param oldRiskScore previous risk score
 * @param newRiskScore updated risk score
 */
public record OutbreakRiskChangedEvent(
    UUID outbreakId,
    String diseaseName,
    OutbreakRiskScore oldRiskScore,
    OutbreakRiskScore newRiskScore
) {}
