package app.vetra.animal.dto;

import app.vetra.infrastructure.persistence.entity.AnimalHealthRecord;
import app.vetra.infrastructure.persistence.enums.HealthRecordSource;
import app.vetra.infrastructure.persistence.enums.HealthRecordType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/** Response DTO representing an entry in an animal's lifetime health timeline. */
public record AnimalHealthRecordDto(
    UUID id,
    UUID animalId,
    HealthRecordType recordType,
    HealthRecordSource source,
    String title,
    String description,
    String symptoms,
    String diagnosis,
    String treatment,
    UUID veterinarianId,
    String veterinarianName,
    String documentUrl,
    String vaccineName,
    LocalDate nextDueDate,
    String batchNumber,
    UUID medicalRecordId,
    UUID appointmentId,
    LocalDateTime recordedAt,
    LocalDateTime createdAt) {

  /** Converts an AnimalHealthRecord entity to AnimalHealthRecordDto. */
  public static AnimalHealthRecordDto fromEntity(AnimalHealthRecord record) {
    return new AnimalHealthRecordDto(
        record.getId(),
        record.getAnimal() != null ? record.getAnimal().getId() : null,
        record.getRecordType(),
        record.getSource(),
        record.getTitle(),
        record.getDescription(),
        record.getSymptoms(),
        record.getDiagnosis(),
        record.getTreatment(),
        record.getVeterinarianId(),
        record.getVeterinarianName(),
        record.getDocumentUrl(),
        record.getVaccineName(),
        record.getNextDueDate(),
        record.getBatchNumber(),
        record.getMedicalRecordId(),
        record.getAppointmentId(),
        record.getRecordedAt(),
        record.getCreatedAt() != null
            ? LocalDateTime.ofInstant(record.getCreatedAt(), ZoneOffset.UTC)
            : record.getRecordedAt());
  }
}
