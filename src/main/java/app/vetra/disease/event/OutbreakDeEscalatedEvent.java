package app.vetra.disease.event;

import app.vetra.disease.entity.OutbreakRiskScore;
import java.util.UUID;

/**
 * Event published when an outbreak cluster risk score de-escalates (e.g. HIGH -> MEDIUM).
 *
 * @param outbreakId outbreak UUID
 * @param diseaseName disease name
 * @param newRiskScore de-escalated risk score
 */
public record OutbreakDeEscalatedEvent(
    UUID outbreakId, String diseaseName, OutbreakRiskScore newRiskScore) {}
