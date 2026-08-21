package app.vetra.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.vetra.auth.dto.FarmerRegisterRequest;
import app.vetra.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:vetra_user_controller_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
      "spring.flyway.enabled=false",
      "vetra.jwt.secret=test-jwt-secret-value-minimum-32-characters-long",
      "vetra.jwt.expiration-ms=86400000",
      "vetra.jwt.refresh-expiration-ms=604800000",
      "vetra.cors.allowed-origins=http://localhost:3000",
      "vetra.cors.allowed-methods=GET,POST,PUT,DELETE,PATCH,OPTIONS",
      "vetra.cors.allowed-headers=*",
      "vetra.cors.allow-credentials=true",
      "vetra.cors.max-age=3600",
      "vetra.aws.region=ap-south-1",
      "vetra.aws.credentials.access-key=test-key",
      "vetra.aws.credentials.secret-key=test-secret",
      "vetra.aws.s3.bucket-name=vetra-test-bucket",
      "vetra.aws.s3.presigned-url-expiry-minutes=15",
    })
class UserControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AuthService authService;

  @Test
  @WithMockUser(username = "farmer.marathi@vetra.app", roles = "FARMER")
  @DisplayName("PUT /api/v1/users/preferences/language updates preferred language to Marathi successfully")
  void testUpdateLanguagePreferenceMarathi() throws Exception {
    FarmerRegisterRequest registerReq =
        new FarmerRegisterRequest(
            "farmer.marathi@vetra.app",
            "9898989898",
            "password123",
            "Ramesh Patil",
            "Patil Dairy",
            "Baramati",
            "Pune",
            "Maharashtra",
            18.5204,
            73.8567,
            10,
            "en");
    authService.registerFarmer(registerReq);

    String requestBody = "{\"language\": \"mr\"}";

    mockMvc
        .perform(
            put("/api/v1/users/preferences/language")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.preferredLanguage").value("mr"));
  }

  @Test
  @WithMockUser(username = "farmer.hindi@vetra.app", roles = "FARMER")
  @DisplayName("PUT /api/v1/users/preferences/language updates preferred language to Hindi successfully")
  void testUpdateLanguagePreferenceHindi() throws Exception {
    FarmerRegisterRequest registerReq =
        new FarmerRegisterRequest(
            "farmer.hindi@vetra.app",
            "9898989899",
            "password123",
            "Suresh Kumar",
            "Kumar Farm",
            "Karnal",
            "Karnal",
            "Haryana",
            29.6857,
            76.9905,
            15,
            "en");
    authService.registerFarmer(registerReq);

    String requestBody = "{\"language\": \"hi\"}";

    mockMvc
        .perform(
            put("/api/v1/users/preferences/language")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.preferredLanguage").value("hi"));
  }
}
