package app.vetra.developer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.vetra.developer.controller.DeveloperAnalyticsController;
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
import app.vetra.developer.dto.TelemetryClassification;
import app.vetra.developer.service.DeveloperAnalyticsService;
import app.vetra.infrastructure.persistence.enums.UserRole;
import app.vetra.infrastructure.response.ApiResponse;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeveloperAnalyticsControllerTest {

  private DeveloperAnalyticsService analyticsService;
  private DeveloperAnalyticsController controller;

  @BeforeEach
  void setUp() {
    analyticsService = mock(DeveloperAnalyticsService.class);
    controller = new DeveloperAnalyticsController(analyticsService);
  }

  @Test
  @DisplayName("Should return platform overview with real telemetry tags")
  void shouldReturnPlatformOverview() {
    DeveloperOverviewDto mockDto =
        new DeveloperOverviewDto(
            10L, 5L, 3L, 1L, 1L, 9L, 1L,
            25L, 12L, 2L, 8L, 1L,
            4L, 2L, 2L, 15L, 10L, 8L, 30L,
            3L, 45L, 2L, 95.7,
            "UP", 1200L, "0.12.5.0", "dev", "ALL",
            Map.of("users", TelemetryClassification.REAL_BACKEND_DATA));

    when(analyticsService.getOverview("ALL")).thenReturn(mockDto);

    ApiResponse<DeveloperOverviewDto> response = controller.getOverview("ALL");
    assertNotNull(response);
    assertTrue(response.success());
    assertEquals(10L, response.data().totalUsers());
    assertEquals(5L, response.data().farmersCount());
    assertEquals("UP", response.data().systemStatus());
  }

  @Test
  @DisplayName("Should return paginated user directory")
  void shouldReturnUserDirectory() {
    DeveloperUserPageResponse mockPage =
        new DeveloperUserPageResponse(Collections.emptyList(), 0, 20, 0, 0, false, false);
    when(analyticsService.getUsers(any(), any(), any(), anyInt(), anyInt())).thenReturn(mockPage);

    ApiResponse<DeveloperUserPageResponse> response =
        controller.getUsers(UserRole.FARMER, true, null, 0, 20);
    assertNotNull(response);
    assertTrue(response.success());
    assertEquals(0, response.data().users().size());
  }

  @Test
  @DisplayName("Should return authentication telemetry with safe session metadata")
  void shouldReturnAuthTelemetry() {
    DeveloperAuthAnalyticsDto mockDto =
        new DeveloperAuthAnalyticsDto(
            50L, 48L, 2L, 96.0,
            30L, 1L, 18L, 1L,
            5L, 1L, 60L,
            Collections.emptyList(),
            Map.of("historicalIpAudit", "NOT CURRENTLY TRACKED"));

    when(analyticsService.getAuthAnalytics()).thenReturn(mockDto);

    ApiResponse<DeveloperAuthAnalyticsDto> response = controller.getAuthAnalytics();
    assertNotNull(response);
    assertTrue(response.success());
    assertEquals(50L, response.data().totalLoginAttempts());
    assertEquals(96.0, response.data().overallSuccessRatePercent());
  }

  @Test
  @DisplayName("Should return farmer analytics")
  void shouldReturnFarmerAnalytics() {
    DeveloperFarmerAnalyticsDto mockDto =
        new DeveloperFarmerAnalyticsDto(
            10L, 9L, 2L, 30L,
            Map.of("CATTLE", 20L), 25L, 5L, 83.3,
            4L, 12L, 8L, 15L,
            Map.of("Pune", 5L), Collections.emptyList(), "ALL");

    when(analyticsService.getFarmerAnalytics("ALL")).thenReturn(mockDto);

    ApiResponse<DeveloperFarmerAnalyticsDto> response = controller.getFarmerAnalytics("ALL");
    assertNotNull(response);
    assertEquals(10L, response.data().totalFarmers());
    assertEquals(30L, response.data().totalAnimals());
  }

  @Test
  @DisplayName("Should return veterinarian analytics")
  void shouldReturnVetAnalytics() {
    DeveloperVetAnalyticsDto mockDto =
        new DeveloperVetAnalyticsDto(
            5L, 4L, 3L, 1L, 1L, 3L, 2L,
            15L, 10L, 2L, 1L, 8L,
            6.5, Map.of("Surgery", 2L), Map.of("Pune", 3L),
            Collections.emptyList(), "ALL");

    when(analyticsService.getVetAnalytics("ALL")).thenReturn(mockDto);

    ApiResponse<DeveloperVetAnalyticsDto> response = controller.getVetAnalytics("ALL");
    assertNotNull(response);
    assertEquals(5L, response.data().totalVeterinarians());
    assertEquals(3L, response.data().verifiedVeterinarians());
  }

  @Test
  @DisplayName("Should return feature usage matrix")
  void shouldReturnFeatureUsage() {
    DeveloperFeatureUsageDto mockDto =
        new DeveloperFeatureUsageDto(Collections.emptyList(), 11L, 1L);
    when(analyticsService.getFeatureUsage()).thenReturn(mockDto);

    ApiResponse<DeveloperFeatureUsageDto> response = controller.getFeatureUsage();
    assertNotNull(response);
    assertEquals(11L, response.data().totalTrackedFeatures());
  }

  @Test
  @DisplayName("Should return activity feed")
  void shouldReturnActivityFeed() {
    DeveloperActivityEventDto event =
        new DeveloperActivityEventDto(
            "EVT-001", "USER_REGISTERED", "FARMER", "fa****r@gmail.com",
            "New Farmer Registered", "Registration complete", "ACTIVE",
            Instant.now(), "123e4567-e89b-12d3-a456-426614174000", "AUTH");

    when(analyticsService.getActivityFeed(30)).thenReturn(List.of(event));

    ApiResponse<List<DeveloperActivityEventDto>> response = controller.getActivityFeed(30);
    assertNotNull(response);
    assertEquals(1, response.data().size());
    assertEquals("USER_REGISTERED", response.data().get(0).eventType());
  }

  @Test
  @DisplayName("Should return system health telemetry")
  void shouldReturnSystemHealth() {
    DeveloperSystemHealthDto mockDto =
        new DeveloperSystemHealthDto(
            "UP", Instant.now(), 3600L, "0.12.5.0", "dev",
            "UP", 2, 5, 20, 7, "UP", "UP",
            1000000000L, 2000000000L, 200000000L, 400000000L, 50000000L,
            0.15, 0.05, 24, "ALIVE", "READY",
            Map.of("database", TelemetryClassification.REAL_BACKEND_DATA));

    when(analyticsService.getSystemHealth()).thenReturn(mockDto);

    ApiResponse<DeveloperSystemHealthDto> response = controller.getSystemHealth();
    assertNotNull(response);
    assertEquals("UP", response.data().status());
    assertEquals("ALIVE", response.data().liveness());
  }

  @Test
  @DisplayName("Should return API performance metrics")
  void shouldReturnApiPerformance() {
    DeveloperApiPerformanceDto mockDto =
        new DeveloperApiPerformanceDto(
            150L, 140L, 8L, 2L, 6.67, 45.2, 320.0,
            Collections.emptyList(),
            TelemetryClassification.REAL_BACKEND_DATA,
            "Micrometer metric extraction");

    when(analyticsService.getApiPerformance()).thenReturn(mockDto);

    ApiResponse<DeveloperApiPerformanceDto> response = controller.getApiPerformance();
    assertNotNull(response);
    assertEquals(150L, response.data().totalRequests());
  }

  @Test
  @DisplayName("Should return non-sensitive settings configuration")
  void shouldReturnSettings() {
    DeveloperSettingsDto mockDto =
        new DeveloperSettingsDto(
            "vetra-backend", "dev", "0.12.5.0",
            "PostgreSQL 16 + PostGIS", "HikariPool (Max: 20)",
            "HMAC-SHA256", 86400L, 604800L,
            List.of("http://localhost:3000", "http://localhost:3001"),
            true, "in-memory", true, "gemini", "diagnostics-fast",
            "Micrometer Observation");

    when(analyticsService.getSettings()).thenReturn(mockDto);

    ApiResponse<DeveloperSettingsDto> response = controller.getSettings();
    assertNotNull(response);
    assertEquals("vetra-backend", response.data().applicationName());
    assertEquals("HMAC-SHA256", response.data().jwtAlgorithm());
  }
}
