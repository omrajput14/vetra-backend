package app.vetra.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.auth.repository.UserRepository;
import app.vetra.auth.repository.VetProfileRepository;
import app.vetra.auth.service.VetVerificationService;
import app.vetra.infrastructure.exception.ResourceNotFoundException;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.infrastructure.persistence.enums.UserRole;
import app.vetra.infrastructure.persistence.enums.VerificationStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/** Unit & integration test suite for VetVerificationService verification rules and lifecycle. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:vet_verification_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
class VetVerificationServiceTest {

  @Autowired private VetVerificationService vetVerificationService;
  @Autowired private VetProfileRepository vetProfileRepository;
  @Autowired private UserRepository userRepository;

  private VetProfile pendingVet;
  private VetProfile verifiedVet;
  private VetProfile rejectedVet;

  @BeforeEach
  void setUp() {
    User user1 =
        userRepository.save(
            User.builder()
                .email("pending.vet@vetra.app")
                .passwordHash("hashed")
                .role(UserRole.VETERINARIAN)
                .phone("+919111111111")
                .isActive(true)
                .build());

    pendingVet =
        vetProfileRepository.save(
            VetProfile.builder()
                .user(user1)
                .fullName("Dr. Pending Sharma")
                .registrationNumber("VET-PENDING-001")
                .qualification("BVSc")
                .specialization("General")
                .clinicName("Rural Clinic")
                .verificationStatus(VerificationStatus.PENDING)
                .build());

    User user2 =
        userRepository.save(
            User.builder()
                .email("verified.vet@vetra.app")
                .passwordHash("hashed")
                .role(UserRole.VETERINARIAN)
                .phone("+919222222222")
                .isActive(true)
                .build());

    verifiedVet =
        vetProfileRepository.save(
            VetProfile.builder()
                .user(user2)
                .fullName("Dr. Verified Roy")
                .registrationNumber("VET-VERIFIED-002")
                .qualification("MVSc Surgery")
                .specialization("Bovine Medicine")
                .clinicName("Roy Animal Care")
                .verificationStatus(VerificationStatus.VERIFIED)
                .build());

    User user3 =
        userRepository.save(
            User.builder()
                .email("rejected.vet@vetra.app")
                .passwordHash("hashed")
                .role(UserRole.VETERINARIAN)
                .phone("+919333333333")
                .isActive(true)
                .build());

    rejectedVet =
        vetProfileRepository.save(
            VetProfile.builder()
                .user(user3)
                .fullName("Dr. Rejected Rao")
                .registrationNumber("VET-REJECTED-003")
                .qualification("Diploma")
                .specialization("None")
                .clinicName("Unlicensed Facility")
                .verificationStatus(VerificationStatus.REJECTED)
                .build());
  }

  @Test
  @DisplayName("isVerified correctly evaluates verification status")
  void testIsVerified() {
    assertFalse(vetVerificationService.isVerified(pendingVet.getId()));
    assertTrue(vetVerificationService.isVerified(verifiedVet.getId()));
    assertFalse(vetVerificationService.isVerified(rejectedVet.getId()));
    assertFalse(vetVerificationService.isVerified(UUID.randomUUID()));
  }

  @Test
  @DisplayName("verifyVeterinarian transitions status to VERIFIED")
  void testVerifyVeterinarian() {
    VetProfile updated = vetVerificationService.verifyVeterinarian(pendingVet.getId());
    assertNotNull(updated);
    assertEquals(VerificationStatus.VERIFIED, updated.getVerificationStatus());
    assertTrue(updated.isVerified());
    assertTrue(vetVerificationService.isVerified(pendingVet.getId()));
  }

  @Test
  @DisplayName("rejectVeterinarian transitions status to REJECTED")
  void testRejectVeterinarian() {
    VetProfile updated =
        vetVerificationService.rejectVeterinarian(pendingVet.getId(), "Invalid registration license");
    assertNotNull(updated);
    assertEquals(VerificationStatus.REJECTED, updated.getVerificationStatus());
    assertFalse(updated.isVerified());
  }

  @Test
  @DisplayName("getVerifiedVeterinarians retrieves only verified profiles")
  void testGetVerifiedVeterinarians() {
    List<VetProfile> verifiedList = vetVerificationService.getVerifiedVeterinarians();
    assertNotNull(verifiedList);
    assertTrue(verifiedList.stream().anyMatch(v -> v.getId().equals(verifiedVet.getId())));
    assertFalse(verifiedList.stream().anyMatch(v -> v.getId().equals(pendingVet.getId())));
    assertFalse(verifiedList.stream().anyMatch(v -> v.getId().equals(rejectedVet.getId())));
  }

  @Test
  @DisplayName("updateVerificationStatus throws ResourceNotFoundException for unknown vet ID")
  void testUpdateVerificationStatus_NotFound() {
    assertThrows(
        ResourceNotFoundException.class,
        () -> vetVerificationService.updateVerificationStatus(UUID.randomUUID(), VerificationStatus.VERIFIED));
  }
}
