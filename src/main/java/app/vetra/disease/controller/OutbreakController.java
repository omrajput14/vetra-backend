package app.vetra.disease.controller;

import app.vetra.disease.dto.DiseaseAnalyticsResponse;
import app.vetra.disease.dto.DiseaseReportResponse;
import app.vetra.disease.dto.OperationalAlertResponse;
import app.vetra.disease.dto.OutbreakResponse;
import app.vetra.disease.dto.OutbreakStatisticsResponse;
import app.vetra.disease.dto.VaccinationAnalyticsResponse;
import app.vetra.disease.entity.OutbreakStatus;
import app.vetra.disease.geo.GeoJsonFeatureCollection;
import app.vetra.disease.geo.GeoJsonService;
import app.vetra.disease.geo.HeatmapPoint;
import app.vetra.disease.registry.DiseaseMetadata;
import app.vetra.disease.registry.DiseaseRegistryService;
import app.vetra.disease.service.DiseaseAnalyticsService;
import app.vetra.disease.service.DiseaseService;
import app.vetra.disease.service.OperationalAlertService;
import app.vetra.disease.service.VaccinationAnalyticsService;
import app.vetra.infrastructure.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller providing endpoints for outbreak cluster intelligence, GeoJSON map
 * exports, spatial heatmaps, vaccination intelligence, and operational alert management.
 */
@RestController
@RequestMapping("/api/v1/disease")
@Tag(
    name = "Disease Intelligence & Operations",
    description =
        "Endpoints for outbreak cluster intelligence, GeoJSON exports, alerts, and vaccination analytics")
@SecurityRequirement(name = "bearerAuth")
public class OutbreakController {

  private final DiseaseService diseaseService;
  private final GeoJsonService geoJsonService;
  private final DiseaseAnalyticsService analyticsService;
  private final DiseaseRegistryService registryService;
  private final VaccinationAnalyticsService vaccinationAnalyticsService;
  private final OperationalAlertService operationalAlertService;

  /** Constructor injection. */
  public OutbreakController(
      DiseaseService diseaseService,
      GeoJsonService geoJsonService,
      DiseaseAnalyticsService analyticsService,
      DiseaseRegistryService registryService,
      VaccinationAnalyticsService vaccinationAnalyticsService,
      OperationalAlertService operationalAlertService) {
    this.diseaseService = diseaseService;
    this.geoJsonService = geoJsonService;
    this.analyticsService = analyticsService;
    this.registryService = registryService;
    this.vaccinationAnalyticsService = vaccinationAnalyticsService;
    this.operationalAlertService = operationalAlertService;
  }

  /** Lists active or historical disease outbreaks. */
  @GetMapping("/outbreaks")
  @Operation(
      summary = "List Outbreak Clusters",
      description = "Retrieves active or historical disease outbreak clusters.")
  public ApiResponse<List<OutbreakResponse>> listOutbreaks(
      @RequestParam(value = "status", required = false) OutbreakStatus status) {
    List<OutbreakResponse> response = diseaseService.listOutbreaks(status);
    return ApiResponse.ok("Outbreak clusters retrieved successfully", response);
  }

  /** Retrieves high-risk outbreak clusters (HIGH or CRITICAL risk scores). */
  @GetMapping("/outbreaks/high-risk")
  @Operation(
      summary = "Get High Risk Outbreaks",
      description =
          "Retrieves active outbreak clusters with HIGH or CRITICAL risk severity scores.")
  public ApiResponse<List<OutbreakResponse>> getHighRiskOutbreaks() {
    List<OutbreakResponse> response = diseaseService.getHighRiskOutbreaks();
    return ApiResponse.ok("High-risk outbreak clusters retrieved successfully", response);
  }

  /** Generates epidemiological outbreak cluster summary statistics. */
  @GetMapping("/outbreaks/statistics")
  @Operation(
      summary = "Get Outbreak Statistics",
      description =
          "Retrieves summary epidemiological statistics for active and historical outbreaks.")
  public ApiResponse<OutbreakStatisticsResponse> getOutbreakStatistics() {
    OutbreakStatisticsResponse response = diseaseService.getOutbreakStatistics();
    return ApiResponse.ok("Outbreak statistics retrieved successfully", response);
  }

  /** Retrieves outbreak velocity trends. */
  @GetMapping("/outbreaks/trends")
  @Operation(
      summary = "Get Outbreak Velocity Trends",
      description = "Retrieves active outbreak clusters with spatial-temporal velocity trends.")
  public ApiResponse<List<OutbreakResponse>> getOutbreakTrends() {
    List<OutbreakResponse> response = diseaseService.listOutbreaks(null);
    return ApiResponse.ok("Outbreak velocity trends retrieved successfully", response);
  }

  /** Exports active outbreaks as an RFC 7946 compliant GeoJSON FeatureCollection. */
  @GetMapping("/outbreaks/geojson")
  @Operation(
      summary = "Get Outbreaks GeoJSON",
      description =
          "Exports active outbreak clusters as a valid RFC 7946 GeoJSON FeatureCollection.")
  public ApiResponse<GeoJsonFeatureCollection> getOutbreaksGeoJson() {
    GeoJsonFeatureCollection response = geoJsonService.getOutbreaksGeoJson();
    return ApiResponse.ok("Outbreaks GeoJSON exported successfully", response);
  }

  /** Exports spatial heatmap hotspot dataset with normalized intensity weights. */
  @GetMapping("/outbreaks/heatmap")
  @Operation(
      summary = "Get Outbreaks Spatial Heatmap",
      description =
          "Exports spatial heatmap hotspot dataset with normalized intensity weights (0.0 to 1.0).")
  public ApiResponse<List<HeatmapPoint>> getOutbreaksHeatmap() {
    List<HeatmapPoint> response = geoJsonService.getHeatmapData();
    return ApiResponse.ok("Spatial heatmap dataset exported successfully", response);
  }

  /** Generates comprehensive epidemiological analytics metrics. */
  @GetMapping("/analytics")
  @Operation(
      summary = "Get Disease Analytics",
      description = "Retrieves comprehensive epidemiological analytics and surveillance metrics.")
  public ApiResponse<DiseaseAnalyticsResponse> getDiseaseAnalytics() {
    DiseaseAnalyticsResponse response = analyticsService.getAnalytics();
    return ApiResponse.ok("Disease analytics metrics retrieved successfully", response);
  }

  /** Retrieves disease taxonomy metadata registry. */
  @GetMapping("/registry")
  @Operation(
      summary = "Get Disease Registry",
      description = "Retrieves disease taxonomy metadata catalog.")
  public ApiResponse<List<DiseaseMetadata>> getDiseaseRegistry() {
    List<DiseaseMetadata> response = registryService.getAllDiseases();
    return ApiResponse.ok("Disease taxonomy metadata registry retrieved successfully", response);
  }

  /** Retrieves outbreak cluster details by ID. */
  @GetMapping("/outbreaks/{id}")
  @Operation(
      summary = "Get Outbreak Details by ID",
      description = "Retrieves outbreak cluster details by UUID.")
  public ApiResponse<OutbreakResponse> getOutbreakById(@PathVariable("id") UUID id) {
    OutbreakResponse response = diseaseService.getOutbreak(id);
    return ApiResponse.ok("Outbreak cluster details retrieved successfully", response);
  }

  /** Retrieves all disease reports contributing to a specific outbreak cluster. */
  @GetMapping("/outbreaks/{id}/reports")
  @Operation(
      summary = "Get Reports for Outbreak Cluster",
      description = "Retrieves all disease reports contributing to an outbreak cluster.")
  public ApiResponse<List<DiseaseReportResponse>> getReportsForOutbreak(
      @PathVariable("id") UUID id) {
    List<DiseaseReportResponse> response = diseaseService.getReportsForOutbreak(id);
    return ApiResponse.ok("Outbreak cluster reports retrieved successfully", response);
  }

  /** Generates regional livestock vaccination intelligence and pathogen coverage metrics. */
  @GetMapping("/vaccination/analytics")
  @Operation(
      summary = "Get Vaccination Intelligence Analytics",
      description = "Retrieves aggregated livestock vaccination coverage and regional immunity gap data.")
  public ApiResponse<VaccinationAnalyticsResponse> getVaccinationAnalytics() {
    VaccinationAnalyticsResponse response = vaccinationAnalyticsService.getVaccinationAnalytics();
    return ApiResponse.ok("Vaccination analytics retrieved successfully", response);
  }

  /** Lists active operational surveillance alerts and critical priority events. */
  @GetMapping("/alerts")
  @Operation(
      summary = "List Operational Surveillance Alerts",
      description = "Retrieves deterministic operational surveillance alerts synthesized from active clusters.")
  public ApiResponse<List<OperationalAlertResponse>> listOperationalAlerts() {
    List<OperationalAlertResponse> response = operationalAlertService.listOperationalAlerts();
    return ApiResponse.ok("Operational alerts retrieved successfully", response);
  }

  /** Retrieves details of a specific operational surveillance alert by UUID. */
  @GetMapping("/alerts/{id}")
  @Operation(
      summary = "Get Operational Alert by ID",
      description = "Retrieves details of a specific operational surveillance alert by UUID.")
  public ApiResponse<OperationalAlertResponse> getAlertById(@PathVariable("id") UUID id) {
    OperationalAlertResponse response = operationalAlertService.getAlertById(id);
    return ApiResponse.ok("Operational alert details retrieved successfully", response);
  }
}
