package app.vetra.disease.event;

import app.vetra.disease.entity.DiagnosisStatus;
import java.util.UUID;

/**
 * Event published when a new disease report is submitted.
 *
 * @param reportId report UUID
 * @param animalId target animal UUID
 * @param diseaseName disease name
 * @param status diagnosis status
 * @param latitude latitude coordinate
 * @param longitude longitude coordinate
 */
public record DiseaseReportCreatedEvent(
    UUID reportId,
    UUID animalId,
    String diseaseName,
    DiagnosisStatus status,
    Double latitude,
    Double longitude) {}
