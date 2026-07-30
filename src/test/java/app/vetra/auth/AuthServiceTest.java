package app.vetra.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.auth.dto.AuthResponse;
import app.vetra.auth.dto.ChangePasswordRequest;
import app.vetra.auth.dto.FarmerRegisterRequest;
import app.vetra.auth.dto.LoginRequest;
import app.vetra.auth.dto.RefreshTokenRequest;
import app.vetra.auth.dto.VetRegisterRequest;
import app.vetra.auth.repository.RefreshTokenRepository;
import app.vetra.auth.service.AuthService;
import app.vetra.auth.service.RefreshTokenService;
import app.vetra.infrastructure.exception.UnauthorizedResourceAccessException;
import app.vetra.infrastructure.persistence.entity.RefreshToken;
import app.vetra.infrastructure.persistence.enums.UserRole;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration & unit tests for AuthService and Secure SHA-256 Refresh Token storage.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:vetra_sha256_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
class AuthServiceTest {

  @Autowired
  private AuthService authService;

  @Autowired
  private RefreshTokenService refreshTokenService;

  @Autowired
  private RefreshTokenRepository refreshTokenRepository;

  @Test
  void testFarmerRegistrationAndSha256TokenStorage() {
    FarmerRegisterRequest registerRequest = new FarmerRegisterRequest(
        "farmer@vetra.app",
        "+1555019283",
        "secret123",
        "John Farmer",
        "Green Valley Farm",
        "Oak Village",
        "Central District",
        "State Region",
        12.34,
        56.78,
        25
    );

    AuthResponse regResponse = authService.registerFarmer(registerRequest);
    String rawRefreshToken = regResponse.refreshToken();

    assertNotNull(regResponse.accessToken());
    assertNotNull(rawRefreshToken);
    assertEquals(UserRole.FARMER, regResponse.user().role());

    // Verify SHA-256 hash storage in DB
    String expectedHash = refreshTokenService.hashToken(rawRefreshToken);
    assertEquals(64, expectedHash.length());
    assertNotEquals(rawRefreshToken, expectedHash);

    Optional<RefreshToken> dbToken = refreshTokenRepository.findByTokenHash(expectedHash);
    assertTrue(dbToken.isPresent());
    assertEquals(expectedHash, dbToken.get().getTokenHash());
  }

  @Test
  void testRefreshTokenRotationWithHashedTokens() {
    VetRegisterRequest registerRequest = new VetRegisterRequest(
        "dr.jenkins@vetra.app",
        "+1555019883",
        "vetpass123",
        "Dr. Sarah Jenkins",
        "VET-REG-9941",
        "BVSc & AH",
        "Ruminant Surgery",
        "Valley Vet Hospital",
        12,
        12.35,
        56.79
    );

    AuthResponse regResponse = authService.registerVet(registerRequest);
    String oldRawToken = regResponse.refreshToken();

    AuthResponse refreshResponse = authService.refreshToken(new RefreshTokenRequest(oldRawToken));
    String newRawToken = refreshResponse.refreshToken();

    assertNotNull(refreshResponse.accessToken());
    assertNotEquals(oldRawToken, newRawToken);

    // Old token should be invalidated/deleted
    assertThrows(UnauthorizedResourceAccessException.class, () ->
        authService.refreshToken(new RefreshTokenRequest(oldRawToken)));
  }

  @Test
  void testPasswordChangeRevokesAllSessions() {
    FarmerRegisterRequest registerRequest = new FarmerRegisterRequest(
        "security@vetra.app",
        "+1555019999",
        "oldpass123",
        "Test Security User",
        "Test Farm",
        "Village",
        "District",
        "State",
        0.0,
        0.0,
        5
    );

    AuthResponse regResponse = authService.registerFarmer(registerRequest);
    String rawRefreshToken = regResponse.refreshToken();

    // Password change must revoke all refresh sessions
    authService.changePassword("security@vetra.app", new ChangePasswordRequest("oldpass123", "newpass456"));

    // Attempting to refresh with old token must fail
    assertThrows(UnauthorizedResourceAccessException.class, () ->
        authService.refreshToken(new RefreshTokenRequest(rawRefreshToken)));

    // New login with new password works
    AuthResponse newLogin = authService.loginFarmer(new LoginRequest("security@vetra.app", "newpass456"));
    assertNotNull(newLogin.accessToken());
  }
}
