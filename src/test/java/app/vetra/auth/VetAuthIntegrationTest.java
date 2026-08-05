package app.vetra.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.vetra.auth.dto.AuthResponse;
import app.vetra.auth.dto.LoginRequest;
import app.vetra.auth.dto.VetRegisterRequest;
import app.vetra.auth.service.AuthService;
import app.vetra.dashboard.dto.DashboardResponse;
import app.vetra.dashboard.service.DashboardService;
import app.vetra.infrastructure.persistence.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regression and integration test suite for Veterinarian Authentication, Role Mapping, and
 * Dashboard Telemetry.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:vetra_vet_auth_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
class VetAuthIntegrationTest {

  @Autowired private AuthService authService;

  @Autowired private DashboardService dashboardService;

  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  void testVetRegistrationAndLoginFlow() {
    VetRegisterRequest regReq =
        new VetRegisterRequest(
            "dr.jenkins@vetra.app",
            "+1555019999",
            "vetpass123",
            "Dr. Sarah Jenkins",
            "VET-9941-XX",
            "BVSc & AH",
            "Large Animals",
            "Valley Veterinary Clinic",
            10,
            12.0,
            77.0);

    AuthResponse regResp = authService.registerVet(regReq);
    assertNotNull(regResp.accessToken());
    assertNotNull(regResp.refreshToken());
    assertEquals(UserRole.VETERINARIAN, regResp.user().role());
    assertEquals("Dr. Sarah Jenkins", regResp.user().fullName());
    assertEquals("Valley Veterinary Clinic", regResp.user().clinicName());

    LoginRequest loginReq = new LoginRequest("dr.jenkins@vetra.app", "vetpass123");
    AuthResponse loginResp = authService.loginVet(loginReq);
    assertNotNull(loginResp.accessToken());
    assertEquals(UserRole.VETERINARIAN, loginResp.user().role());

    DashboardResponse dash = dashboardService.getDashboardMetrics("dr.jenkins@vetra.app");
    assertEquals("Dr. Sarah Jenkins", dash.userName());
    assertEquals("Valley Veterinary Clinic", dash.facilityName());
    assertEquals("VETERINARIAN", dash.role());
  }
}
