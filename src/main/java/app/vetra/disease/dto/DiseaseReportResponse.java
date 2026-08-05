package app.vetra.disease.dto;

import app.vetra.disease.entity.DiagnosisConfidenceSource;
import app.vetra.disease.entity.DiagnosisStatus;
import app.vetra.disease.entity.DiseaseReport;
import app.vetra.disease.entity.DiseaseReportSource;
import java.time.Instant;
import java.util.UUID;

/** Public DTO representing a disease surveillance report. */
public record DiseaseReportResponse(
    UUID id,
    UUID animalId,
    String tagNumber,
    String animalName,
    UUID medicalRecordId,
    UUID aiScanId,
    UUID reportedById,
    String reportedByName,
    DiseaseReportSource reportSource,
    DiagnosisConfidenceSource diagnosisConfidenceSource,
    String diseaseName,
    DiagnosisStatus diagnosisStatus,
    Double latitude,
    Double longitude,
    String notes,
    Instant createdAt,
    Instant updatedAt) {

  /**
   * Factory method mapping DiseaseReport entity to DiseaseReportResponse DTO.
   *
   * @param report entity instance
   * @return {@link DiseaseReportResponse} DTO
   */
  public static DiseaseReportResponse fromEntity(DiseaseReport report) {
    return new DiseaseReportResponse(
        report.getId(),
        report.getAnimal().getId(),
        report.getAnimal().getTagNumber(),
        report.getAnimal().getAnimalName(),
        report.getMedicalRecord() != null ? report.getMedicalRecord().getId() : null,
        report.getAiScan() != null ? report.getAiScan().getId() : null,
        report.getReportedBy().getId(),
        report.getReportedBy().getEmail(),
        report.getReportSource(),
        report.getDiagnosisConfidenceSource(),
        report.getDiseaseName(),
        report.getDiagnosisStatus(),
        report.getLatitude(),
        report.getLongitude(),
        report.getNotes(),
        report.getCreatedAt(),
        report.getUpdatedAt());
  }
}
