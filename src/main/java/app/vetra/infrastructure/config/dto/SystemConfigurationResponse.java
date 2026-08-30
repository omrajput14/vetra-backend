package app.vetra.infrastructure.config.dto;

/**
 * DTO encapsulating non-sensitive, read-only system and surveillance configuration metadata
 * exposed to authorized government officers.
 */
public record SystemConfigurationResponse(
    SurveillanceConfigDto surveillance,
    WeatherConfigDto weather,
    AlertConfigDto alerts,
    SystemMetadataDto system) {

  /** Surveillance engine configuration parameters. */
  public record SurveillanceConfigDto(
      double weightCluster,
      double weightWeather,
      double weightHistory,
      double weightVaccination,
      int lowThreshold,
      int mediumThreshold,
      int highThreshold,
      double confirmedCaseMultiplier,
      double suspectedCaseMultiplier) {}

  /** Weather service integration parameters. */
  public record WeatherConfigDto(
      String provider,
      boolean enabled,
      int cacheTtlMinutes,
      int timeoutSeconds,
      int connectTimeoutSeconds,
      String apiEndpoint) {}

  /** Operational alert derivation parameters. */
  public record AlertConfigDto(
      int epidemiologicalRiskThreshold,
      String operationalPriorityFormula,
      String evaluationMode) {}

  /** Infrastructure and deployment runtime metadata. */
  public record SystemMetadataDto(
      String serviceName,
      String environment,
      String version,
      String databaseEngine,
      String connectionPool,
      String securityStandard,
      String auditTelemetry) {}
}
