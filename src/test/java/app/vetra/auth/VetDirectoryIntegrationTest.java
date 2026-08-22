package app.vetra.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.auth.dto.AuthResponse;
import app.vetra.auth.dto.VetRegisterRequest;
import app.vetra.auth.dto.VetSummaryDto;
import app.vetra.auth.repository.UserRepository;
import app.vetra.auth.repository.VetProfileRepository;
import app.vetra.auth.service.AuthService;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.infrastructure.persistence.enums.UserRole;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test for Veterinarian Directory, Phone Contact exposure, and Emergency Availability.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:vetra_vet_dir_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
class VetDirectoryIntegrationTest {

  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepository;
  @Autowired private VetProfileRepository vetProfileRepository;

  @Test
  void testVetDirectoryReturnsContactInfoAndEmergencyAvailability() {
    VetRegisterRequest regReq =
        new VetRegisterRequest(
            "dr.ananya@vetra.app",
            "+919876543210",
            "password123",
            "Dr. Ananya Roy",
            "VET-WB-2024-88",
            "BVSc & AH",
            "Bovine Medicine & Surgery",
            "Roy Animal Hospital",
            8,
            22.57,
            88.36);

    AuthResponse authResp = authService.registerVet(regReq);
    assertNotNull(authResp);

    List<VetSummaryDto> vets = authService.listVeterinarians();
    assertNotNull(vets);
    assertTrue(vets.stream().anyMatch(v -> "Dr. Ananya Roy".equals(v.name())));

    VetSummaryDto vet =
        vets.stream().filter(v -> "Dr. Ananya Roy".equals(v.name())).findFirst().orElseThrow();

    assertEquals("Dr. Ananya Roy", vet.name());
    assertEquals("Dr. Ananya Roy", vet.fullName());
    assertEquals("Roy Animal Hospital", vet.clinic());
    assertEquals("Roy Animal Hospital", vet.clinicName());
    assertEquals("+919876543210", vet.phoneNumber());
    assertEquals("+919876543210", vet.phone());
    assertEquals("Bovine Medicine & Surgery", vet.specialization());
    assertEquals("BVSc & AH", vet.qualification());
    assertEquals(5.0, vet.rating());
    assertTrue(vet.isAvailable());
    assertTrue(vet.emergencyAvailable());
  }

  @Test
  void testVetDirectoryHandlesMissingPhoneNumberGracefully() {
    User user =
        User.builder()
            .email("dr.nophone@vetra.app")
            .phone(null)
            .passwordHash("hashed")
            .role(UserRole.VETERINARIAN)
            .isActive(true)
            .build();
    user = userRepository.save(user);

    VetProfile profile =
        VetProfile.builder()
            .user(user)
            .fullName("Dr. No Phone")
            .registrationNumber("VET-TEST-999")
            .clinicName("Rural Clinic")
            .qualification("BVSc")
            .specialization("General")
            .yearsExperience(5)
            .isAvailable(true)
            .emergencyAvailable(true)
            .build();
    vetProfileRepository.save(profile);

    List<VetSummaryDto> vets = authService.listVeterinarians();
    assertNotNull(vets);
    assertTrue(vets.stream().anyMatch(v -> "Dr. No Phone".equals(v.name())));

    VetSummaryDto vet =
        vets.stream().filter(v -> "Dr. No Phone".equals(v.name())).findFirst().orElseThrow();
    assertEquals(null, vet.phoneNumber());
    assertEquals("Rural Clinic", vet.clinic());
  }
}
