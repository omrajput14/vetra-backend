package app.vetra.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regression test suite verifying that farmer registration validation constraints,
 * error envelopes, and authentication work as specified.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:vetra_farmer_val_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
class FarmerAuthValidationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("POST /api/v1/auth/farmer/register fails with 400 when email is invalid")
  void testRegistrationFailsWithInvalidEmail() throws Exception {
    String payload = """
        {
          "email": "invalid-email-format",
          "password": "Password@123",
          "fullName": "Test Farmer"
        }
        """;

    mockMvc
        .perform(
            post("/api/v1/auth/farmer/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Request validation failed"))
        .andExpect(jsonPath("$.errors[?(@.field == 'email')].message").exists());
  }

  @Test
  @DisplayName("POST /api/v1/auth/farmer/register fails with 400 when password is under 6 characters")
  void testRegistrationFailsWithShortPassword() throws Exception {
    String payload = """
        {
          "email": "farmer@example.com",
          "password": "123",
          "fullName": "Test Farmer"
        }
        """;

    mockMvc
        .perform(
            post("/api/v1/auth/farmer/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Request validation failed"))
        .andExpect(jsonPath("$.errors[?(@.field == 'password')].message").exists());
  }

  @Test
  @DisplayName("POST /api/v1/auth/farmer/register succeeds with valid data without phone")
  void testRegistrationSucceedsWithoutPhone() throws Exception {
    String payload = """
        {
          "email": "farmer.valid@vetra.app",
          "password": "Password@123",
          "fullName": "Sanjay Patil",
          "district": "Dhule",
          "state": "Maharashtra",
          "taluka": "Dhule",
          "animalCount": 5
        }
        """;

    mockMvc
        .perform(
            post("/api/v1/auth/farmer/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.status").value(201))
        .andExpect(jsonPath("$.data.accessToken").isString())
        .andExpect(jsonPath("$.data.refreshToken").isString())
        .andExpect(jsonPath("$.data.user.role").value("FARMER"))
        .andExpect(jsonPath("$.data.user.fullName").value("Sanjay Patil"));
  }

  @Test
  @DisplayName("POST /api/v1/auth/farmer/login succeeds with registered farmer credentials")
  void testLoginSucceedsAfterRegistration() throws Exception {
    String registerPayload = """
        {
          "email": "login.farmer@vetra.app",
          "password": "Password@123",
          "fullName": "Login Farmer",
          "district": "Dhule",
          "state": "Maharashtra"
        }
        """;

    mockMvc
        .perform(
            post("/api/v1/auth/farmer/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerPayload))
        .andExpect(status().isCreated());

    String loginPayload = """
        {
          "identifier": "login.farmer@vetra.app",
          "password": "Password@123"
        }
        """;

    mockMvc
        .perform(
            post("/api/v1/auth/farmer/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.accessToken").isString())
        .andExpect(jsonPath("$.data.user.role").value("FARMER"));
  }
}
