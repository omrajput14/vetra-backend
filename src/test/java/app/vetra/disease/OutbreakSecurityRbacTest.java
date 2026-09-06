package app.vetra.disease;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class OutbreakSecurityRbacTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  @WithMockUser(roles = "FARMER")
  @DisplayName("FARMER receives 403 Forbidden for protected government surveillance analytics")
  void farmerForbiddenFromGovernmentAnalytics() throws Exception {
    mockMvc.perform(get("/api/v1/disease/analytics"))
        .andExpect(status().isForbidden());

    mockMvc.perform(get("/api/v1/disease/outbreaks/statistics"))
        .andExpect(status().isForbidden());

    mockMvc.perform(get("/api/v1/disease/alerts"))
        .andExpect(status().isForbidden());

    mockMvc.perform(get("/api/v1/disease/vaccination/analytics"))
        .andExpect(status().isForbidden());

    mockMvc.perform(get("/api/v1/disease/outbreaks"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "GOVERNMENT_OFFICER")
  @DisplayName("GOVERNMENT_OFFICER receives 200 OK for government surveillance analytics")
  void governmentOfficerAllowedForAnalytics() throws Exception {
    mockMvc.perform(get("/api/v1/disease/analytics"))
        .andExpect(status().isOk());

    mockMvc.perform(get("/api/v1/disease/outbreaks/statistics"))
        .andExpect(status().isOk());

    mockMvc.perform(get("/api/v1/disease/alerts"))
        .andExpect(status().isOk());

    mockMvc.perform(get("/api/v1/disease/vaccination/analytics"))
        .andExpect(status().isOk());

    mockMvc.perform(get("/api/v1/disease/outbreaks"))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "VETERINARIAN")
  @DisplayName("VETERINARIAN allowed on outbreaks but forbidden from high-level government analytics")
  void veterinarianAccessRules() throws Exception {
    mockMvc.perform(get("/api/v1/disease/outbreaks"))
        .andExpect(status().isOk());

    mockMvc.perform(get("/api/v1/disease/analytics"))
        .andExpect(status().isForbidden());
  }
}
