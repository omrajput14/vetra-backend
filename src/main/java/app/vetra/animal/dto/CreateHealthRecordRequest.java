package app.vetra.animal.dto;

import app.vetra.infrastructure.persistence.enums.HealthRecordSource;
import app.vetra.infrastructure.persistence.enums.HealthRecordType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

/** Request DTO for creating a new health record in an animal's medical timeline. */
public record CreateHealthRecordRequest(
    @NotNull(message = "Record type is required") HealthRecordType recordType,
    HealthRecordSource source,
    @NotBlank(message = "Title is required") String title,
    String description,
    String symptoms,
    String diagnosis,
    String treatment,
    UUID veterinarianId,
    String veterinarianName,
    LocalDateTime recordedAt) {}
