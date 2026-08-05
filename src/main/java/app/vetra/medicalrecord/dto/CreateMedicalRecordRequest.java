package app.vetra.medicalrecord.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Request payload for creating an Electronic Veterinary Medical Record. */
public record CreateMedicalRecordRequest(
    @NotNull(message = "Appointment ID is required") UUID appointmentId,
    @NotBlank(message = "Diagnosis is required") String diagnosis,
    String symptoms,
    @NotBlank(message = "Treatment details are required") String treatment,
    String prescription,
    BigDecimal weight,
    BigDecimal temperature,
    @FutureOrPresent(message = "Follow-up date cannot be in the past") LocalDate followUpDate,
    String notes) {}
