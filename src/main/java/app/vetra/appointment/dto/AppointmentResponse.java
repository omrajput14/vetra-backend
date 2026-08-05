package app.vetra.appointment.dto;

import app.vetra.infrastructure.persistence.entity.Appointment;
import app.vetra.infrastructure.persistence.enums.AppointmentStatus;
import app.vetra.infrastructure.persistence.enums.VisitType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Response DTO representing appointment details. */
public record AppointmentResponse(
    UUID id,
    UUID farmerId,
    String farmerName,
    String farmerPhone,
    UUID veterinarianId,
    String veterinarianName,
    String clinicName,
    UUID animalId,
    String animalName,
    String tagNumber,
    String species,
    LocalDate appointmentDate,
    LocalTime appointmentTime,
    VisitType visitType,
    String reason,
    AppointmentStatus status,
    String veterinarianNotes,
    String cancellationReason,
    Long version,
    Instant createdAt,
    Instant updatedAt) {

  /** Factory method to convert an entity to response DTO. */
  public static AppointmentResponse fromEntity(Appointment appointment) {
    String farmerPhone =
        appointment.getFarmer() != null && appointment.getFarmer().getUser() != null
            ? appointment.getFarmer().getUser().getPhone()
            : null;

    return new AppointmentResponse(
        appointment.getId(),
        appointment.getFarmer() != null ? appointment.getFarmer().getId() : null,
        appointment.getFarmer() != null ? appointment.getFarmer().getFullName() : null,
        farmerPhone,
        appointment.getVeterinarian() != null ? appointment.getVeterinarian().getId() : null,
        appointment.getVeterinarian() != null ? appointment.getVeterinarian().getFullName() : null,
        appointment.getVeterinarian() != null
            ? appointment.getVeterinarian().getClinicName()
            : null,
        appointment.getAnimal() != null ? appointment.getAnimal().getId() : null,
        appointment.getAnimal() != null ? appointment.getAnimal().getAnimalName() : null,
        appointment.getAnimal() != null ? appointment.getAnimal().getTagNumber() : null,
        appointment.getAnimal() != null && appointment.getAnimal().getSpecies() != null
            ? appointment.getAnimal().getSpecies().name()
            : null,
        appointment.getAppointmentDate(),
        appointment.getAppointmentTime(),
        appointment.getVisitType(),
        appointment.getReason(),
        appointment.getStatus(),
        appointment.getVeterinarianNotes(),
        appointment.getCancellationReason(),
        appointment.getVersion(),
        appointment.getCreatedAt(),
        appointment.getUpdatedAt());
  }
}
