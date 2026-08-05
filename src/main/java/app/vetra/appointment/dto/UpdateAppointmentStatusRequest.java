package app.vetra.appointment.dto;

import app.vetra.infrastructure.persistence.enums.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

/** DTO for updating the status of an appointment. */
public record UpdateAppointmentStatusRequest(
    @NotNull(message = "Status is required") AppointmentStatus status,
    String notes,
    String cancellationReason) {}
