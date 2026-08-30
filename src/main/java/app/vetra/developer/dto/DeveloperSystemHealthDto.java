package app.vetra.developer.dto;

import java.time.Instant;
import java.util.Map;

/** Live Spring Boot Actuator and JVM infrastructure telemetry payload. */
public record DeveloperSystemHealthDto(
    String status,
    Instant timestamp,
    long uptimeSeconds,
    String version,
    String environment,
    String databaseStatus,
    int databasePoolActive,
    int databasePoolIdle,
    int databasePoolMax,
    int databasePoolTotal,
    String redisStatus,
    String diskStatus,
    long diskFreeBytes,
    long diskTotalBytes,
    long jvmHeapUsedBytes,
    long jvmHeapMaxBytes,
    long jvmNonHeapUsedBytes,
    double systemCpuLoad,
    double processCpuLoad,
    int activeThreadCount,
    String liveness,
    String readiness,
    Map<String, TelemetryClassification> componentIntegrity) {}
