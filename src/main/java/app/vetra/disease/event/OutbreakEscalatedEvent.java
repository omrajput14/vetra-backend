package app.vetra.disease.event;

import app.vetra.disease.entity.OutbreakRiskScore;
import java.util.UUID;

/**
 * Event published when an outbreak cluster risk score escalates (e.g. MEDIUM -> HIGH or CRITICAL).
 *
 * @param outbreakId outbreak UUID
 * @param diseaseName disease name
 * @param newRiskScore escalated risk score
 * @param affectedReportsCount total affected cases count
 */
public record OutbreakEscalatedEvent(
    UUID outbreakId,
    String diseaseName,
    OutbreakRiskScore newRiskScore,
    int affectedReportsCount) {}
