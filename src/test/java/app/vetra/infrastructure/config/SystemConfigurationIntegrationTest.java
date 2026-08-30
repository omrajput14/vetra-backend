package app.vetra.infrastructure.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SystemConfigurationIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  @WithMockUser(roles = "GOVERNMENT_OFFICER")
  @DisplayName("GET /api/v1/system/configuration returns 200 OK for GOVERNMENT_OFFICER")
  void shouldReturn200ForGovernmentOfficer() throws Exception {
    mockMvc
        .perform(get("/api/v1/system/configuration"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.surveillance.weightCluster").value(0.40))
        .andExpect(jsonPath("$.data.weather.provider").value("Open-Meteo Meteorological API"))
        .andExpect(jsonPath("$.data.alerts.epidemiologicalRiskThreshold").value(80))
        .andExpect(jsonPath("$.data.system.databaseEngine").isNotEmpty());
  }

  @Test
  @WithMockUser(roles = "FARMER")
  @DisplayName("GET /api/v1/system/configuration returns 403 Forbidden for FARMER")
  void shouldReturn403ForFarmer() throws Exception {
    mockMvc
        .perform(get("/api/v1/system/configuration"))
        .andDo(print())
        .andExpect(status().isForbidden());
  }
}
