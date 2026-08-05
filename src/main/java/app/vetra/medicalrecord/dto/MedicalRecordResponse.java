package app.vetra.medicalrecord.dto;

import app.vetra.infrastructure.persistence.entity.MedicalRecord;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Response payload representing an Electronic Veterinary Medical Record. */
public record MedicalRecordResponse(
    UUID id,
    UUID appointmentId,
    UUID animalId,
    String animalName,
    String tagNumber,
    String species,
    UUID farmerId,
    String farmerName,
    UUID veterinarianId,
    String veterinarianName,
    String clinicName,
    String diagnosis,
    String symptoms,
    String treatment,
    String prescription,
    BigDecimal weight,
    BigDecimal temperature,
    LocalDate followUpDate,
    String notes,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    Long version) {
  /** Converts a MedicalRecord entity into MedicalRecordResponse DTO. */
  public static MedicalRecordResponse fromEntity(MedicalRecord record) {
    return new MedicalRecordResponse(
        record.getId(),
        record.getAppointment() != null ? record.getAppointment().getId() : null,
        record.getAnimal() != null ? record.getAnimal().getId() : null,
        record.getAnimal() != null ? record.getAnimal().getAnimalName() : null,
        record.getAnimal() != null ? record.getAnimal().getTagNumber() : null,
        record.getAnimal() != null && record.getAnimal().getSpecies() != null
            ? record.getAnimal().getSpecies().name()
            : null,
        record.getFarmer() != null ? record.getFarmer().getId() : null,
        record.getFarmer() != null ? record.getFarmer().getFullName() : null,
        record.getVeterinarian() != null ? record.getVeterinarian().getId() : null,
        record.getVeterinarian() != null ? record.getVeterinarian().getFullName() : null,
        record.getVeterinarian() != null ? record.getVeterinarian().getClinicName() : null,
        record.getDiagnosis(),
        record.getSymptoms(),
        record.getTreatment(),
        record.getPrescription(),
        record.getWeight(),
        record.getTemperature(),
        record.getFollowUpDate(),
        record.getNotes(),
        record.getCreatedAt(),
        record.getUpdatedAt(),
        record.getVersion());
  }
}
