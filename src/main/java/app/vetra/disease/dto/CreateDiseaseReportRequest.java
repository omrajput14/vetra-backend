package app.vetra.disease.dto;

import app.vetra.disease.entity.DiagnosisStatus;
import app.vetra.disease.entity.DiseaseReportSource;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request payload for creating a new disease surveillance report.
 */
public record CreateDiseaseReportRequest(
    @NotNull(message = "Animal ID is required")
    UUID animalId,

    UUID medicalRecordId,

    UUID aiScanId,

    @NotNull(message = "Report source is required")
    DiseaseReportSource reportSource,

    @NotBlank(message = "Disease name is required")
    @Size(max = 128, message = "Disease name cannot exceed 128 characters")
    String diseaseName,

    @NotNull(message = "Diagnosis status is required")
    DiagnosisStatus diagnosisStatus,

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90.0")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90.0")
    Double latitude,

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180.0")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180.0")
    Double longitude,

    @Size(max = 5000, message = "Notes cannot exceed 5000 characters")
    String notes
) {}
