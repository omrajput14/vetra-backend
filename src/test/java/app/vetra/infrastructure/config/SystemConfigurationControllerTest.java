package app.vetra.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.disease.risk.RiskScoreProperties;
import app.vetra.infrastructure.config.dto.SystemConfigurationResponse;
import app.vetra.infrastructure.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SystemConfigurationControllerTest {

  private RiskScoreProperties riskScoreProperties;
  private SystemConfigurationController controller;

  @BeforeEach
  void setUp() {
    riskScoreProperties = new RiskScoreProperties();
    riskScoreProperties.setWeightCluster(0.40);
    riskScoreProperties.setWeightWeather(0.20);
    riskScoreProperties.setWeightHistory(0.20);
    riskScoreProperties.setWeightVaccination(0.20);
    riskScoreProperties.setLowThreshold(30);
    riskScoreProperties.setMediumThreshold(55);
    riskScoreProperties.setHighThreshold(80);
    riskScoreProperties.setWeatherCacheTtlMinutes(30);
    riskScoreProperties.setWeatherTimeoutSeconds(3);
    riskScoreProperties.setWeatherConnectTimeoutSeconds(2);
    riskScoreProperties.setWeatherEnabled(true);
    riskScoreProperties.setWeatherApiBaseUrl("https://api.open-meteo.com/v1/forecast");

    controller = new SystemConfigurationController(riskScoreProperties);
  }

  @Test
  @DisplayName("Should return valid non-sensitive system configuration")
  void shouldReturnValidSystemConfiguration() {
    ApiResponse<SystemConfigurationResponse> apiResponse = controller.getSystemConfiguration();

    assertNotNull(apiResponse);
    assertTrue(apiResponse.success());
    assertEquals("System configuration retrieved successfully", apiResponse.message());

    SystemConfigurationResponse response = apiResponse.data();
    assertNotNull(response);

    // Surveillance verification
    assertEquals(0.40, response.surveillance().weightCluster());
    assertEquals(0.20, response.surveillance().weightWeather());
    assertEquals(0.20, response.surveillance().weightHistory());
    assertEquals(0.20, response.surveillance().weightVaccination());
    assertEquals(30, response.surveillance().lowThreshold());
    assertEquals(55, response.surveillance().mediumThreshold());
    assertEquals(80, response.surveillance().highThreshold());

    // Weather verification
    assertEquals("Open-Meteo Meteorological API", response.weather().provider());
    assertTrue(response.weather().enabled());
    assertEquals(30, response.weather().cacheTtlMinutes());
    assertEquals(3, response.weather().timeoutSeconds());

    // Alerts verification
    assertEquals(80, response.alerts().epidemiologicalRiskThreshold());
    assertNotNull(response.alerts().operationalPriorityFormula());

    // System metadata verification
    assertNotNull(response.system().serviceName());
    assertNotNull(response.system().securityStandard());
  }
}
