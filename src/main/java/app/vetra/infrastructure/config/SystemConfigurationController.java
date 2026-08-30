package app.vetra.infrastructure.config;

import app.vetra.disease.risk.RiskScoreProperties;
import app.vetra.infrastructure.config.dto.SystemConfigurationResponse;
import app.vetra.infrastructure.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller providing authenticated government access to read-only system and surveillance
 * configuration metadata.
 */
@RestController
@RequestMapping("/api/v1/system")
@Tag(
    name = "System Configuration & Telemetry",
    description = "Read-only configuration introspection for authorized government command center")
@SecurityRequirement(name = "bearerAuth")
public class SystemConfigurationController {

  private final RiskScoreProperties riskScoreProperties;

  @Value("${spring.application.name:vetra-backend}")
  private String applicationName = "vetra-backend";

  @Value("${spring.profiles.active:dev}")
  private String environment = "dev";

  @Value("${info.app.version:0.12.5.0}")
  private String version = "0.12.5.0";

  /** Constructor injection. */
  public SystemConfigurationController(RiskScoreProperties riskScoreProperties) {
    this.riskScoreProperties = riskScoreProperties;
  }

  /**
   * Retrieves read-only operational configuration parameters across surveillance, weather, alert
   * engine, and infrastructure layers.
   *
   * @return {@link ApiResponse} containing {@link SystemConfigurationResponse}
   */
  @GetMapping("/configuration")
  @PreAuthorize("hasAnyRole('GOVERNMENT_OFFICER', 'ADMINISTRATOR')")
  @Operation(
      summary = "Get System & Surveillance Configuration",
      description =
          "Returns non-sensitive operational parameters, risk thresholds, and service parameters")
  public ApiResponse<SystemConfigurationResponse> getSystemConfiguration() {
    SystemConfigurationResponse.SurveillanceConfigDto surveillance =
        new SystemConfigurationResponse.SurveillanceConfigDto(
            riskScoreProperties.getWeightCluster(),
            riskScoreProperties.getWeightWeather(),
            riskScoreProperties.getWeightHistory(),
            riskScoreProperties.getWeightVaccination(),
            riskScoreProperties.getLowThreshold(),
            riskScoreProperties.getMediumThreshold(),
            riskScoreProperties.getHighThreshold(),
            riskScoreProperties.getConfirmedCaseWeight(),
            riskScoreProperties.getSuspectedCaseWeight());

    SystemConfigurationResponse.WeatherConfigDto weather =
        new SystemConfigurationResponse.WeatherConfigDto(
            "Open-Meteo Meteorological API",
            riskScoreProperties.isWeatherEnabled(),
            riskScoreProperties.getWeatherCacheTtlMinutes(),
            riskScoreProperties.getWeatherTimeoutSeconds(),
            riskScoreProperties.getWeatherConnectTimeoutSeconds(),
            riskScoreProperties.getWeatherApiBaseUrl());

    SystemConfigurationResponse.AlertConfigDto alerts =
        new SystemConfigurationResponse.AlertConfigDto(
            80,
            "0.50 * CompositeRisk + 0.30 * VaccinationGap + 0.20 * CaseVelocity",
            "Dynamic Deterministic Derivation (Read-Only)");

    SystemConfigurationResponse.SystemMetadataDto system =
        new SystemConfigurationResponse.SystemMetadataDto(
            applicationName != null ? applicationName : "vetra-backend",
            environment != null ? environment : "dev",
            version != null ? version : "0.12.5.0",
            "PostgreSQL 16 + PostGIS 3.4 (Hibernate Spatial)",
            "VetraHikariPool (Max: 20, MinIdle: 5)",
            "Stateless 256-bit JWT (HMAC-SHA256)",
            "Micrometer + OpenTelemetry Distributed Tracing");

    SystemConfigurationResponse response =
        new SystemConfigurationResponse(surveillance, weather, alerts, system);

    return ApiResponse.ok("System configuration retrieved successfully", response);
  }
}
