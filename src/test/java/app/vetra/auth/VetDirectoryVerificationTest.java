package app.vetra.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.auth.dto.AuthResponse;
import app.vetra.auth.dto.VetRegisterRequest;
import app.vetra.auth.dto.VetSummaryDto;
import app.vetra.auth.repository.UserRepository;
import app.vetra.auth.repository.VetProfileRepository;
import app.vetra.auth.service.AuthService;
import app.vetra.auth.service.VetVerificationService;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.infrastructure.persistence.enums.UserRole;
import app.vetra.infrastructure.persistence.enums.VerificationStatus;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * End-to-end integration test verifying Verified Veterinarian discovery rules:
 * - VERIFIED veterinarians appear in public directory with verified = true
 * - PENDING registered veterinarians are hidden from public directory
 * - REJECTED veterinarians are hidden from public directory
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:vet_dir_verif_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
class VetDirectoryVerificationTest {

  @Autowired private AuthService authService;
  @Autowired private VetVerificationService vetVerificationService;
  @Autowired private UserRepository userRepository;
  @Autowired private VetProfileRepository vetProfileRepository;

  @Test
  @DisplayName("Newly registered vet is PENDING and hidden from directory until VERIFIED")
  void testNewVetIsPendingAndHiddenUntilVerified() {
    VetRegisterRequest regReq =
        new VetRegisterRequest(
            "dr.rahul@vetra.app",
            "+919876543299",
            "password123",
            "Dr. Rahul Sharma",
            "VET-MH-2026-999",
            "MVSc Medicine",
            "Bovine Medicine",
            "Pune Vet Hospital",
            7,
            18.52,
            73.85);

    AuthResponse authResp = authService.registerVet(regReq);
    assertNotNull(authResp);

    // Fetch profile from DB
    User user = userRepository.findByEmail("dr.rahul@vetra.app").orElseThrow();
    VetProfile profile = vetProfileRepository.findByUser(user).orElseThrow();
    assertEquals(VerificationStatus.PENDING, profile.getVerificationStatus());
    assertFalse(profile.isVerified());

    // Should NOT appear in public directory while PENDING
    List<VetSummaryDto> directoryBefore = authService.listVeterinarians();
    assertFalse(directoryBefore.stream().anyMatch(v -> "Dr. Rahul Sharma".equals(v.name())));

    // Admin verifies the veterinarian
    vetVerificationService.verifyVeterinarian(profile.getId());

    // Should now appear in public directory with verified status
    List<VetSummaryDto> directoryAfter = authService.listVeterinarians();
    assertTrue(directoryAfter.stream().anyMatch(v -> "Dr. Rahul Sharma".equals(v.name())));

    VetSummaryDto verifiedDto =
        directoryAfter.stream().filter(v -> "Dr. Rahul Sharma".equals(v.name())).findFirst().orElseThrow();

    assertEquals("Dr. Rahul Sharma", verifiedDto.name());
    assertEquals(VerificationStatus.VERIFIED, verifiedDto.verificationStatus());
    assertTrue(verifiedDto.verified());
    assertEquals("Pune Vet Hospital", verifiedDto.clinic());
    assertEquals("+919876543299", verifiedDto.phoneNumber());
  }

  @Test
  @DisplayName("Rejected veterinarian is hidden from public directory")
  void testRejectedVetIsHiddenFromDirectory() {
    User user =
        userRepository.save(
            User.builder()
                .email("dr.fraud@vetra.app")
                .passwordHash("hashed")
                .role(UserRole.VETERINARIAN)
                .phone("+919999999999")
                .isActive(true)
                .build());

    VetProfile profile =
        vetProfileRepository.save(
            VetProfile.builder()
                .user(user)
                .fullName("Fake Doctor")
                .registrationNumber("VET-FAKE-000")
                .clinicName("Fraud Clinic")
                .qualification("None")
                .specialization("None")
                .yearsExperience(0)
                .isAvailable(true)
                .verificationStatus(VerificationStatus.REJECTED)
                .build());

    List<VetSummaryDto> directory = authService.listVeterinarians();
    assertFalse(directory.stream().anyMatch(v -> "Fake Doctor".equals(v.name())));
  }
}
