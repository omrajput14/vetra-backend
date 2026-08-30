package app.vetra.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

/** Integration & unit tests for AuthService and Secure SHA-256 Refresh Token storage. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(
    properties = {
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

  @Autowired private AuthService authService;

  @Autowired private RefreshTokenService refreshTokenService;

  @Autowired private RefreshTokenRepository refreshTokenRepository;

  @Test
  void testFarmerRegistrationAndSha256TokenStorage() {
    FarmerRegisterRequest registerRequest =
        new FarmerRegisterRequest(
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
            25);

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
    VetRegisterRequest registerRequest =
        new VetRegisterRequest(
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
            56.79);

    AuthResponse regResponse = authService.registerVet(registerRequest);
    String oldRawToken = regResponse.refreshToken();

    AuthResponse refreshResponse = authService.refreshToken(new RefreshTokenRequest(oldRawToken));
    String newRawToken = refreshResponse.refreshToken();

    assertNotNull(refreshResponse.accessToken());
    assertNotEquals(oldRawToken, newRawToken);

    // Old token should be invalidated/deleted
    assertThrows(
        UnauthorizedResourceAccessException.class,
        () -> authService.refreshToken(new RefreshTokenRequest(oldRawToken)));
  }

  @Test
  void testPasswordChangeRevokesAllSessions() {
    FarmerRegisterRequest registerRequest =
        new FarmerRegisterRequest(
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
            5);

    AuthResponse regResponse = authService.registerFarmer(registerRequest);
    String rawRefreshToken = regResponse.refreshToken();

    // Password change must revoke all refresh sessions
    authService.changePassword(
        "security@vetra.app", new ChangePasswordRequest("oldpass123", "newpass456"));

    // Attempting to refresh with old token must fail
    assertThrows(
        UnauthorizedResourceAccessException.class,
        () -> authService.refreshToken(new RefreshTokenRequest(rawRefreshToken)));

    // New login with new password works
    AuthResponse newLogin =
        authService.loginFarmer(new LoginRequest("security@vetra.app", "newpass456"));
    assertNotNull(newLogin.accessToken());
  }

  @Test
  void testFarmerRegistrationWithTalukaAndCoordinates() {
    FarmerRegisterRequest request =
        new FarmerRegisterRequest(
            "taluka.farmer@vetra.app",
            "+919876547001",
            "securePass123",
            "Ramesh Patel",
            "Patel Dairy Farm",
            "Vadodara Village",
            "Karjan",
            "Vadodara",
            "Gujarat",
            22.01234,
            73.12345,
            15,
            "hi");

    AuthResponse response = authService.registerFarmer(request);
    assertNotNull(response.accessToken());
    assertNotNull(response.user());
    assertEquals("Ramesh Patel", response.user().fullName());
    assertEquals("Vadodara Village", response.user().village());
    assertEquals("Karjan", response.user().taluka());
    assertEquals("Vadodara", response.user().district());
    assertEquals("Gujarat", response.user().state());
    assertEquals(22.01234, response.user().latitude());
    assertEquals(73.12345, response.user().longitude());
    assertEquals("hi", response.user().preferredLanguage());
  }

  @Test
  void testFarmerRegistrationWithoutTalukaAndNullCoordinates() {
    // Tests backward compatibility and GPS permission denied / unavailable scenario
    FarmerRegisterRequest request =
        new FarmerRegisterRequest(
            "notaluka.farmer@vetra.app",
            "+919876547002",
            "securePass123",
            "Suresh Kumar",
            "Kumar Farm",
            "Rampur",
            "Karnal",
            "Haryana",
            null,
            null,
            8);

    AuthResponse response = authService.registerFarmer(request);
    assertNotNull(response.accessToken());
    assertNotNull(response.user());
    assertEquals("Suresh Kumar", response.user().fullName());
    assertEquals("Rampur", response.user().village());
    org.junit.jupiter.api.Assertions.assertNull(response.user().taluka());
    assertEquals("Karnal", response.user().district());
    assertEquals("Haryana", response.user().state());
    org.junit.jupiter.api.Assertions.assertNull(response.user().latitude());
    org.junit.jupiter.api.Assertions.assertNull(response.user().longitude());
  }

  @Test
  void testFarmerProfileUpdateWithTalukaAndCoordinates() {
    FarmerRegisterRequest request =
        new FarmerRegisterRequest(
            "update.farmer@vetra.app",
            "+919876547003",
            "securePass123",
            "Initial Name",
            "Initial Farm",
            "Old Village",
            "Old District",
            "Old State",
            null,
            null,
            10);

    authService.registerFarmer(request);

    // Update with Taluka and Device Coordinates
    app.vetra.auth.dto.UpdateProfileRequest updateRequest =
        new app.vetra.auth.dto.UpdateProfileRequest(
            "Updated Name",
            "+919876547003",
            "Updated Farm",
            "New Village",
            "New Taluka",
            "New District",
            "New State",
            19.0760,
            72.8777,
            null,
            null,
            null,
            null);

    app.vetra.auth.dto.UserProfileDto updatedProfile =
        authService.updateUserProfile("update.farmer@vetra.app", updateRequest);

    assertEquals("Updated Name", updatedProfile.fullName());
    assertEquals("New Village", updatedProfile.village());
    assertEquals("New Taluka", updatedProfile.taluka());
    assertEquals("New District", updatedProfile.district());
    assertEquals("New State", updatedProfile.state());
    assertEquals(19.0760, updatedProfile.latitude());
    assertEquals(72.8777, updatedProfile.longitude());
  }

  @Test
  void testLocationCoordinateValidationConstraints() {
    jakarta.validation.ValidatorFactory factory =
        jakarta.validation.Validation.buildDefaultValidatorFactory();
    jakarta.validation.Validator validator = factory.getValidator();

    // Valid coordinates
    FarmerRegisterRequest validReq =
        new FarmerRegisterRequest(
            "valid.geo@vetra.app",
            "+1555123456",
            "password123",
            "Geo Farmer",
            "Geo Farm",
            "Village",
            "Taluka",
            "District",
            "State",
            45.0,
            90.0,
            5,
            "en");
    assertTrue(validator.validate(validReq).isEmpty());

    // Invalid latitude (> 90.0)
    FarmerRegisterRequest invalidLatReq =
        new FarmerRegisterRequest(
            "invalid.lat@vetra.app",
            "+1555123456",
            "password123",
            "Geo Farmer",
            "Geo Farm",
            "Village",
            "Taluka",
            "District",
            "State",
            95.0,
            90.0,
            5,
            "en");
    assertEquals(1, validator.validate(invalidLatReq).size());

    // Invalid latitude (< -90.0)
    FarmerRegisterRequest invalidLatNegReq =
        new FarmerRegisterRequest(
            "invalid.latneg@vetra.app",
            "+1555123456",
            "password123",
            "Geo Farmer",
            "Geo Farm",
            "Village",
            "Taluka",
            "District",
            "State",
            -95.0,
            90.0,
            5,
            "en");
    assertEquals(1, validator.validate(invalidLatNegReq).size());

    // Invalid longitude (> 180.0)
    FarmerRegisterRequest invalidLngReq =
        new FarmerRegisterRequest(
            "invalid.lng@vetra.app",
            "+1555123456",
            "password123",
            "Geo Farmer",
            "Geo Farm",
            "Village",
            "Taluka",
            "District",
            "State",
            45.0,
            185.0,
            5,
            "en");
    assertEquals(1, validator.validate(invalidLngReq).size());

    // Invalid longitude (< -180.0)
    FarmerRegisterRequest invalidLngNegReq =
        new FarmerRegisterRequest(
            "invalid.lngneg@vetra.app",
            "+1555123456",
            "password123",
            "Geo Farmer",
            "Geo Farm",
            "Village",
            "Taluka",
            "District",
            "State",
            45.0,
            -185.0,
            5,
            "en");
    assertEquals(1, validator.validate(invalidLngNegReq).size());
  }

  @Test
  void testUpdateVetProfileDutyAndEmergencyAvailabilityDisabled() {
    app.vetra.auth.dto.VetRegisterRequest regReq =
        new app.vetra.auth.dto.VetRegisterRequest(
            "duty.vet@vetra.app",
            "+919876500001",
            "password123",
            "Dr. Duty Test",
            "REG-DUTY-01",
            "BVSc",
            "Surgery",
            "City Clinic",
            "Main Street",
            "Baramati",
            "Baramati",
            "Pune",
            "Maharashtra",
            5,
            18.5204,
            73.8567,
            "en");

    authService.registerVet(regReq);

    // Disable both General Consultation and Emergency Response
    app.vetra.auth.dto.UpdateProfileRequest updateReq =
        new app.vetra.auth.dto.UpdateProfileRequest(
            "Dr. Duty Test",
            "+919876500001",
            null,
            "Baramati",
            "Baramati",
            "Pune",
            "Maharashtra",
            18.5204,
            73.8567,
            "City Clinic",
            "Main Street",
            "Surgery",
            "BVSc",
            5,
            false,
            false,
            "{\"monday\":{\"isWorking\":false}}",
            null,
            null);

    app.vetra.auth.dto.UserProfileDto updatedProfile =
        authService.updateUserProfile("duty.vet@vetra.app", updateReq);

    assertFalse(updatedProfile.isAvailable());
    assertFalse(updatedProfile.available());
    assertFalse(updatedProfile.emergencyAvailable());
    assertFalse(updatedProfile.isEmergencyAvailable());
  }
}
