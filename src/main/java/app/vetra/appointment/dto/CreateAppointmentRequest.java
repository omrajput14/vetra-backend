package app.vetra.appointment.dto;

import app.vetra.infrastructure.persistence.enums.VisitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** DTO for creating a new appointment. */
public record CreateAppointmentRequest(
    @NotNull(message = "Animal ID is required") UUID animalId,
    @NotNull(message = "Veterinarian ID is required") UUID veterinarianId,
    @NotNull(message = "Appointment date is required") LocalDate appointmentDate,
    @NotNull(message = "Appointment time is required") LocalTime appointmentTime,
    @NotNull(message = "Visit type is required") VisitType visitType,
    @NotBlank(message = "Reason is required") String reason) {}
