package app.vetra.disease.dto;

import java.time.Instant;
import java.util.UUID;

/** Response DTO representing an operational surveillance alert or critical epidemiological event. */
public record OperationalAlertResponse(
    UUID id,
    String eventType,
    String title,
    String diseaseName,
    String locationName,
    Double latitude,
    Double longitude,
    String severity,
    Double compositeRiskScore,
    Double vaccinationGapScore,
    Integer affectedCasesCount,
    Instant detectedAt,
    String source,
    String status,
    String whyItMatters,
    String recommendedNextStep,
    UUID relatedOutbreakId,
    UUID relatedReportId) {}
