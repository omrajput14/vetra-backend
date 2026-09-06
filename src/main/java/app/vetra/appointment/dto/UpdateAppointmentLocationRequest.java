package app.vetra.appointment.dto;

import jakarta.validation.constraints.NotNull;

/** DTO for updating veterinarian live coordinates during an active EN_ROUTE appointment. */
public record UpdateAppointmentLocationRequest(
    @NotNull(message = "Latitude is required") Double latitude,
    @NotNull(message = "Longitude is required") Double longitude,
    Double accuracy) {}
