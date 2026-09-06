package app.vetra.dashboard.controller;

import app.vetra.dashboard.dto.DashboardResponse;
import app.vetra.dashboard.service.DashboardService;
import app.vetra.infrastructure.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.vetra.dashboard.dto.EconomicImpactResponse;
import app.vetra.dashboard.service.EconomicImpactService;
import org.springframework.security.access.prepost.PreAuthorize;

/** REST Controller for unified dashboard telemetry and economic impact analytics. */
@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(
    name = "Dashboard Telemetry",
    description = "Endpoints for retrieving unified dashboard statistics and economic metrics")
public class DashboardController {

  private final DashboardService dashboardService;
  private final EconomicImpactService economicImpactService;

  /** Constructor injection. */
  public DashboardController(
      DashboardService dashboardService,
      EconomicImpactService economicImpactService) {
    this.dashboardService = dashboardService;
    this.economicImpactService = economicImpactService;
  }

  /** Retrieves aggregated dashboard metrics in a single API request. */
  @GetMapping
  @Operation(
      summary = "Get Unified Dashboard Metrics",
      description =
          "Returns animal counts, appointments, alerts, and user information in a single response")
  public ApiResponse<DashboardResponse> getDashboardMetrics(Principal principal) {
    DashboardResponse response = dashboardService.getDashboardMetrics(principal.getName());
    return ApiResponse.ok("Dashboard statistics retrieved successfully", response);
  }

  /**
   * Retrieves role-aware modeled economic savings and loss-avoidance impact.
   * Farmers receive their farm's modeled estimate; Government officers and Administrators
   * receive statewide aggregate metrics.
   */
  @GetMapping("/economic-impact")
  @PreAuthorize("hasAnyRole('FARMER', 'GOVERNMENT_OFFICER', 'ADMINISTRATOR', 'VETERINARIAN')")
  @Operation(
      summary = "Get Role-Aware Economic Impact",
      description = "Retrieves modeled economic savings based on caller role privileges")
  public ApiResponse<EconomicImpactResponse> getEconomicImpact(Principal principal) {
    EconomicImpactResponse response =
        economicImpactService.getEconomicImpactForUser(principal.getName());
    return ApiResponse.ok("Economic impact metrics retrieved successfully", response);
  }

  /** Retrieves farm-level modeled economic savings specifically for the authenticated farmer. */
  @GetMapping("/economic-impact/farmer")
  @PreAuthorize("hasRole('FARMER')")
  @Operation(
      summary = "Get Farmer Economic Savings",
      description = "Retrieves farm-level modeled savings from early detection for authenticated farmer")
  public ApiResponse<EconomicImpactResponse> getFarmerEconomicImpact(Principal principal) {
    EconomicImpactResponse response =
        economicImpactService.getFarmerEconomicImpact(principal.getName());
    return ApiResponse.ok("Farmer economic savings retrieved successfully", response);
  }

  /** Retrieves statewide aggregate modeled economic savings across all registered livestock. */
  @GetMapping("/economic-impact/statewide")
  @PreAuthorize("hasAnyRole('GOVERNMENT_OFFICER', 'ADMINISTRATOR')")
  @Operation(
      summary = "Get Statewide Economic Impact",
      description = "Retrieves statewide aggregated modeled savings from early detection")
  public ApiResponse<EconomicImpactResponse> getStatewideEconomicImpact() {
    EconomicImpactResponse response = economicImpactService.getStatewideEconomicImpact();
    return ApiResponse.ok("Statewide economic impact retrieved successfully", response);
  }
}

