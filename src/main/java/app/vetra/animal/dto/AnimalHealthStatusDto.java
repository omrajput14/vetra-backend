package app.vetra.animal.dto;

import app.vetra.infrastructure.persistence.enums.HealthRecordSource;
import app.vetra.infrastructure.persistence.enums.HealthRecordType;
import java.time.LocalDateTime;
import java.util.UUID;

/** Response DTO dynamically computed from the latest animal health timeline event. */
public record AnimalHealthStatusDto(
    UUID animalId,
    String status,
    String statusSummary,
    HealthRecordType latestEventType,
    HealthRecordSource latestEventSource,
    LocalDateTime latestEventTime,
    String latestDiagnosis,
    String latestTreatment) {}
