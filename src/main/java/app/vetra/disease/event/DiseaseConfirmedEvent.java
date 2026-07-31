package app.vetra.disease.event;

import java.util.UUID;

/**
 * Event published when a disease diagnosis is officially confirmed.
 *
 * @param reportId report UUID
 * @param animalId target animal UUID
 * @param diseaseName confirmed disease name
 * @param latitude latitude coordinate
 * @param longitude longitude coordinate
 */
public record DiseaseConfirmedEvent(
    UUID reportId,
    UUID animalId,
    String diseaseName,
    Double latitude,
    Double longitude
) {}
