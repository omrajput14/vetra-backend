package app.vetra.dashboard.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.vetra.dashboard.dto.EconomicImpactResponse;
import app.vetra.dashboard.service.DashboardService;
import app.vetra.dashboard.service.EconomicImpactService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:vetra_dashboard_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
      "spring.flyway.enabled=false",
      "vetra.jwt.secret=test-jwt-secret-value-minimum-32-characters-long",
      "vetra.jwt.expiration-ms=86400000",
      "vetra.jwt.refresh-expiration-ms=604800000",
    })
class DashboardControllerSecurityTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private DashboardService dashboardService;

  @MockitoBean
  private EconomicImpactService economicImpactService;

  @Test
  @WithMockUser(username = "farmer@vetra.co.in", roles = "FARMER")
  void testFarmerCanAccessOwnEconomicImpact() throws Exception {
    EconomicImpactResponse mockResponse = EconomicImpactResponse.ofCalculated(
        new BigDecimal("18000.00"), 1L, "FARMER");
    when(economicImpactService.getFarmerEconomicImpact("farmer@vetra.co.in")).thenReturn(mockResponse);

    mockMvc.perform(get("/api/v1/dashboard/economic-impact/farmer"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.modeledSavings").value(18000.00))
        .andExpect(jsonPath("$.data.isModeled").value(true))
        .andExpect(jsonPath("$.data.label").value("Modeled estimate"));
  }

  @Test
  @WithMockUser(username = "farmer@vetra.co.in", roles = "FARMER")
  void testFarmerBlockedFromStatewideEconomicImpact() throws Exception {
    // Farmer should receive 403 Forbidden on statewide endpoint
    mockMvc.perform(get("/api/v1/dashboard/economic-impact/statewide"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "officer@vetra.co.in", roles = "GOVERNMENT_OFFICER")
  void testGovernmentOfficerCanAccessStatewideEconomicImpact() throws Exception {
    EconomicImpactResponse mockResponse = EconomicImpactResponse.ofCalculated(
        new BigDecimal("124000.00"), 8L, "STATEWIDE");
    when(economicImpactService.getStatewideEconomicImpact()).thenReturn(mockResponse);

    mockMvc.perform(get("/api/v1/dashboard/economic-impact/statewide"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.modeledSavings").value(124000.00))
        .andExpect(jsonPath("$.data.hasSufficientData").value(true))
        .andExpect(jsonPath("$.data.scope").value("STATEWIDE"));
  }

  @Test
  void testUnauthenticatedUserRejected() throws Exception {
    mockMvc.perform(get("/api/v1/dashboard/economic-impact"))
        .andExpect(status().isUnauthorized());
  }
}
