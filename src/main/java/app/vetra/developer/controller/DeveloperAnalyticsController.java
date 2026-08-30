package app.vetra.developer.controller;

import app.vetra.developer.dto.DeveloperActivityEventDto;
import app.vetra.developer.dto.DeveloperApiPerformanceDto;
import app.vetra.developer.dto.DeveloperAuthAnalyticsDto;
import app.vetra.developer.dto.DeveloperFarmerAnalyticsDto;
import app.vetra.developer.dto.DeveloperFeatureUsageDto;
import app.vetra.developer.dto.DeveloperOverviewDto;
import app.vetra.developer.dto.DeveloperSettingsDto;
import app.vetra.developer.dto.DeveloperSystemHealthDto;
import app.vetra.developer.dto.DeveloperUserPageResponse;
import app.vetra.developer.dto.DeveloperVetAnalyticsDto;
import app.vetra.developer.service.DeveloperAnalyticsService;
import app.vetra.infrastructure.persistence.enums.UserRole;
import app.vetra.infrastructure.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller providing developer and platform administration telemetry.
 * Strictly restricted to authorized users with ADMINISTRATOR role.
 */
@RestController
@RequestMapping("/api/v1/developer")
@Tag(
    name = "Developer Console Telemetry",
    description = "Platform usage, authentication activity, and system health endpoints for Developers/Admins")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class DeveloperAnalyticsController {

  private final DeveloperAnalyticsService analyticsService;

  public DeveloperAnalyticsController(DeveloperAnalyticsService analyticsService) {
    this.analyticsService = analyticsService;
  }

  /** Section 1: Overview KPIs. */
  @GetMapping("/overview")
  @Operation(summary = "Get Platform Overview KPIs", description = "Returns aggregated platform metrics and user counts")
  public ApiResponse<DeveloperOverviewDto> getOverview(
      @RequestParam(required = false, defaultValue = "ALL") String timeRange) {
    DeveloperOverviewDto response = analyticsService.getOverview(timeRange);
    return ApiResponse.ok("Platform overview retrieved successfully", response);
  }

  /** Section 2: User Directory. */
  @GetMapping("/users")
  @Operation(summary = "Get User Directory", description = "Returns paginated user list with role and status filtering")
  public ApiResponse<DeveloperUserPageResponse> getUsers(
      @RequestParam(required = false) UserRole role,
      @RequestParam(required = false) Boolean isActive,
      @RequestParam(required = false) String search,
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "20") int size) {
    DeveloperUserPageResponse response = analyticsService.getUsers(role, isActive, search, page, size);
    return ApiResponse.ok("User directory retrieved successfully", response);
  }

  /** Section 3: Authentication Telemetry. */
  @GetMapping("/analytics/auth")
  @Operation(summary = "Get Authentication Telemetry", description = "Returns login counters, success rates, and safe session metadata")
  public ApiResponse<DeveloperAuthAnalyticsDto> getAuthAnalytics() {
    DeveloperAuthAnalyticsDto response = analyticsService.getAuthAnalytics();
    return ApiResponse.ok("Authentication telemetry retrieved successfully", response);
  }

  /** Section 4: Farmer Analytics. */
  @GetMapping("/analytics/farmers")
  @Operation(summary = "Get Farmer Analytics", description = "Returns farmer usage, livestock breakdown, and registration trends")
  public ApiResponse<DeveloperFarmerAnalyticsDto> getFarmerAnalytics(
      @RequestParam(required = false, defaultValue = "ALL") String timeRange) {
    DeveloperFarmerAnalyticsDto response = analyticsService.getFarmerAnalytics(timeRange);
    return ApiResponse.ok("Farmer analytics retrieved successfully", response);
  }

  /** Section 5: Veterinarian Analytics. */
  @GetMapping("/analytics/veterinarians")
  @Operation(summary = "Get Veterinarian Analytics", description = "Returns vet verification pipeline, availability, and clinical metrics")
  public ApiResponse<DeveloperVetAnalyticsDto> getVetAnalytics(
      @RequestParam(required = false, defaultValue = "ALL") String timeRange) {
    DeveloperVetAnalyticsDto response = analyticsService.getVetAnalytics(timeRange);
    return ApiResponse.ok("Veterinarian analytics retrieved successfully", response);
  }

  /** Section 6: Feature Usage Matrix. */
  @GetMapping("/analytics/features")
  @Operation(summary = "Get Feature Adoption Matrix", description = "Returns feature adoption rates and data tracking classifications")
  public ApiResponse<DeveloperFeatureUsageDto> getFeatureUsage() {
    DeveloperFeatureUsageDto response = analyticsService.getFeatureUsage();
    return ApiResponse.ok("Feature adoption matrix retrieved successfully", response);
  }

  /** Section 7: Activity Feed. */
  @GetMapping("/activity")
  @Operation(summary = "Get Operational Activity Stream", description = "Returns real timestamped events across platform domains")
  public ApiResponse<List<DeveloperActivityEventDto>> getActivityFeed(
      @RequestParam(required = false, defaultValue = "30") int limit) {
    List<DeveloperActivityEventDto> response = analyticsService.getActivityFeed(limit);
    return ApiResponse.ok("Operational activity feed retrieved successfully", response);
  }

  /** Section 8: System Health. */
  @GetMapping("/system/health")
  @Operation(summary = "Get System Health Telemetry", description = "Returns live JVM, database pool, and infrastructure health")
  public ApiResponse<DeveloperSystemHealthDto> getSystemHealth() {
    DeveloperSystemHealthDto response = analyticsService.getSystemHealth();
    return ApiResponse.ok("System health telemetry retrieved successfully", response);
  }

  /** Section 9: API Performance. */
  @GetMapping("/system/api-performance")
  @Operation(summary = "Get API Performance Telemetry", description = "Returns HTTP request throughput, error rates, and route latencies")
  public ApiResponse<DeveloperApiPerformanceDto> getApiPerformance() {
    DeveloperApiPerformanceDto response = analyticsService.getApiPerformance();
    return ApiResponse.ok("API performance telemetry retrieved successfully", response);
  }

  /** Section 10: Strict Developer Settings Allowlist. */
  @GetMapping("/system/settings")
  @Operation(summary = "Get System Settings Configuration", description = "Returns non-sensitive operational config parameters")
  public ApiResponse<DeveloperSettingsDto> getSettings() {
    DeveloperSettingsDto response = analyticsService.getSettings();
    return ApiResponse.ok("System settings retrieved successfully", response);
  }
}
