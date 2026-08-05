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

/** REST Controller for unified dashboard telemetry. */
@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(
    name = "Dashboard Telemetry",
    description = "Endpoints for retrieving unified dashboard statistics in a single call")
public class DashboardController {

  private final DashboardService dashboardService;

  /** Constructor injection. */
  public DashboardController(DashboardService dashboardService) {
    this.dashboardService = dashboardService;
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
}
