package app.vetra.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import app.vetra.auth.dto.VetSummaryDto;
import app.vetra.auth.repository.VetProfileRepository;
import app.vetra.auth.service.impl.VetDiscoveryServiceImpl;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.infrastructure.persistence.enums.UserRole;
import app.vetra.infrastructure.persistence.enums.VerificationStatus;
import app.vetra.infrastructure.persistence.enums.VetMatchType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit test suite for VetDiscoveryServiceImpl. */
@ExtendWith(MockitoExtension.class)
class VetDiscoveryServiceImplTest {

  @Mock private VetProfileRepository vetProfileRepository;

  private VetDiscoveryService vetDiscoveryService;

  @BeforeEach
  void setUp() {
    vetDiscoveryService = new VetDiscoveryServiceImpl(vetProfileRepository);
  }

  private VetProfile createMockVet(
      String fullName,
      String village,
      String taluka,
      String district,
      Double lat,
      Double lng,
      VerificationStatus status) {
    User user =
        User.builder()
            .email(fullName.toLowerCase().replace(" ", ".") + "@example.com")
            .role(UserRole.VETERINARIAN)
            .isActive(true)
            .build();

    return VetProfile.builder()
        .user(user)
        .fullName(fullName)
        .registrationNumber("REG-" + UUID.randomUUID().toString().substring(0, 8))
        .village(village)
        .taluka(taluka)
        .district(district)
        .state("Maharashtra")
        .latitude(lat)
        .longitude(lng)
        .verificationStatus(status)
        .build();
  }

  @Test
  @DisplayName("GPS match takes top priority and sorts by nearest distance")
  void testGpsRadiusMatchPriority() {
    // Farmer at (18.5204, 73.8567) - Pune Center
    // Vet 1 at approx 5 km away
    VetProfile vet1 =
        createMockVet(
            "Dr. Near", "Khed", "Haveli", "Pune", 18.5600, 73.8567, VerificationStatus.VERIFIED);
    // Vet 2 at approx 2 km away
    VetProfile vet2 =
        createMockVet(
            "Dr. Closer",
            "Shivajinagar",
            "Haveli",
            "Pune",
            18.5300,
            73.8567,
            VerificationStatus.PENDING);

    when(vetProfileRepository.findAllActiveNonRejected(any()))
        .thenReturn(List.of(vet1, vet2));

    List<VetSummaryDto> result =
        vetDiscoveryService.searchNearbyVeterinarians(
            18.5204, 73.8567, 25.0, "Khed", "Haveli", "Pune");

    assertEquals(2, result.size());
    assertEquals("Dr. Closer", result.get(0).name());
    assertEquals(VetMatchType.GPS_RADIUS, result.get(0).matchType());
    assertFalse(result.get(0).verified());
    assertEquals(VerificationStatus.PENDING, result.get(0).verificationStatus());

    assertEquals("Dr. Near", result.get(1).name());
    assertEquals(VetMatchType.GPS_RADIUS, result.get(1).matchType());
    assertTrue(result.get(1).verified());
    assertEquals(VerificationStatus.VERIFIED, result.get(1).verificationStatus());
  }

  @Test
  @DisplayName("Matches same village when GPS coordinates are unavailable or outside radius")
  void testSameVillageMatchFallback() {
    VetProfile vetVillage =
        createMockVet(
            "Dr. Village Match",
            "Baramati Rural",
            "Baramati",
            "Pune",
            null,
            null,
            VerificationStatus.PENDING);
    VetProfile vetOther =
        createMockVet(
            "Dr. Other", "Daund Rural", "Daund", "Satara", null, null, VerificationStatus.VERIFIED);

    when(vetProfileRepository.findAllActiveNonRejected(any()))
        .thenReturn(List.of(vetVillage, vetOther));

    // Farmer in Baramati Rural with no GPS coordinates
    List<VetSummaryDto> result =
        vetDiscoveryService.searchNearbyVeterinarians(
            null, null, 50.0, "  baramati   rural ", "Baramati", "Pune");

    assertEquals(1, result.size());
    assertEquals("Dr. Village Match", result.get(0).name());
    assertEquals(VetMatchType.SAME_VILLAGE, result.get(0).matchType());
    assertFalse(result.get(0).verified());
  }

  @Test
  @DisplayName("Matches same taluka fallback when village differs")
  void testSameTalukaMatchFallback() {
    VetProfile vetTaluka =
        createMockVet(
            "Dr. Taluka Match",
            "Village B",
            "Haveli",
            "Pune",
            null,
            null,
            VerificationStatus.VERIFIED);

    when(vetProfileRepository.findAllActiveNonRejected(any()))
        .thenReturn(List.of(vetTaluka));

    List<VetSummaryDto> result =
        vetDiscoveryService.searchNearbyVeterinarians(
            null, null, 50.0, "Village A", "haveli", "Pune");

    assertEquals(1, result.size());
    assertEquals("Dr. Taluka Match", result.get(0).name());
    assertEquals(VetMatchType.SAME_TALUKA, result.get(0).matchType());
    assertTrue(result.get(0).verified());
  }

  @Test
  @DisplayName("Matches same district fallback when taluka and village differ")
  void testSameDistrictMatchFallback() {
    VetProfile vetDistrict =
        createMockVet(
            "Dr. District Match",
            "Village X",
            "Taluka Y",
            "Pune",
            null,
            null,
            VerificationStatus.PENDING);

    when(vetProfileRepository.findAllActiveNonRejected(any()))
        .thenReturn(List.of(vetDistrict));

    List<VetSummaryDto> result =
        vetDiscoveryService.searchNearbyVeterinarians(
            null, null, 50.0, "Village A", "Taluka B", "pune");

    assertEquals(1, result.size());
    assertEquals("Dr. District Match", result.get(0).name());
    assertEquals(VetMatchType.SAME_DISTRICT, result.get(0).matchType());
  }

  @Test
  @DisplayName("Sorts results in strict hierarchy order across multiple matched tiers")
  void testHierarchyOrderingAcrossTiers() {
    VetProfile vetDistrict =
        createMockVet("Dr. District", "Vil 4", "Tal 4", "Pune", null, null, VerificationStatus.VERIFIED);
    VetProfile vetTaluka =
        createMockVet("Dr. Taluka", "Vil 3", "Haveli", "Pune", null, null, VerificationStatus.VERIFIED);
    VetProfile vetVillage =
        createMockVet("Dr. Village", "Khed", "Khed", "Pune", null, null, VerificationStatus.VERIFIED);
    VetProfile vetGps =
        createMockVet("Dr. Gps", "Other", "Other", "Other", 18.5204, 73.8567, VerificationStatus.VERIFIED);

    when(vetProfileRepository.findAllActiveNonRejected(any()))
        .thenReturn(List.of(vetDistrict, vetTaluka, vetVillage, vetGps));

    List<VetSummaryDto> result =
        vetDiscoveryService.searchNearbyVeterinarians(
            18.5204, 73.8567, 10.0, "Khed", "Haveli", "Pune");

    assertEquals(4, result.size());
    assertEquals(VetMatchType.GPS_RADIUS, result.get(0).matchType());
    assertEquals("Dr. Gps", result.get(0).name());
    assertEquals(VetMatchType.SAME_VILLAGE, result.get(1).matchType());
    assertEquals("Dr. Village", result.get(1).name());
    assertEquals(VetMatchType.SAME_TALUKA, result.get(2).matchType());
    assertEquals("Dr. Taluka", result.get(2).name());
    assertEquals(VetMatchType.SAME_DISTRICT, result.get(3).matchType());
    assertEquals("Dr. District", result.get(3).name());
  }

  @Test
  @DisplayName("Returns general list only when no location filters are provided")
  void testGeneralListingWhenNoFilter() {
    VetProfile vet =
        createMockVet(
            "Dr. General",
            "Village A",
            "Taluka A",
            "District A",
            null,
            null,
            VerificationStatus.VERIFIED);

    when(vetProfileRepository.findAllActiveNonRejected(any()))
        .thenReturn(List.of(vet));

    List<VetSummaryDto> result =
        vetDiscoveryService.searchNearbyVeterinarians(null, null, null, null, null, null);

    assertEquals(1, result.size());
    assertEquals(VetMatchType.GENERAL, result.get(0).matchType());
  }

  @Test
  @DisplayName("Strictly excludes geographically unrelated vets when location criteria are provided")
  void testExcludesGeographicallyUnrelatedVetsWhenLocationCriteriaProvided() {
    VetProfile matchedVet =
        createMockVet(
            "Dr. Matched", "Baramati Rural", "Baramati", "Pune", null, null, VerificationStatus.VERIFIED);
    VetProfile unrelatedVet1 =
        createMockVet(
            "Dr. Unrelated Nagpur", "Hingna", "Hingna", "Nagpur", null, null, VerificationStatus.VERIFIED);
    VetProfile unrelatedVet2 =
        createMockVet(
            "Dr. Unrelated Nashik", "Niphad", "Niphad", "Nashik", null, null, VerificationStatus.PENDING);

    when(vetProfileRepository.findAllActiveNonRejected(any()))
        .thenReturn(List.of(matchedVet, unrelatedVet1, unrelatedVet2));

    // Farmer searching within Baramati, Pune
    List<VetSummaryDto> results =
        vetDiscoveryService.searchNearbyVeterinarians(
            null, null, 50.0, "Baramati Rural", "Baramati", "Pune");

    // Only the matching vet should be returned. Unrelated vets must NOT be returned with GENERAL.
    assertEquals(1, results.size());
    assertEquals("Dr. Matched", results.get(0).name());
    assertEquals(VetMatchType.SAME_VILLAGE, results.get(0).matchType());
  }

  @Test
  @DisplayName("Strictly excludes vets outside GPS radius when only GPS criteria are provided")
  void testExcludesVetsOutsideGpsRadiusWithoutGeneralFallback() {
    // Farmer at Pune (18.5204, 73.8567)
    // Vet 1 within 10 km
    VetProfile nearbyVet =
        createMockVet(
            "Dr. Nearby", "Kothrud", "Haveli", "Pune", 18.5074, 73.8077, VerificationStatus.VERIFIED);
    // Vet 2 in Mumbai (19.0760, 72.8777) approx 120 km away
    VetProfile farVet =
        createMockVet(
            "Dr. Far Away", "Dadar", "Mumbai", "Mumbai", 19.0178, 72.8478, VerificationStatus.VERIFIED);

    when(vetProfileRepository.findAllActiveNonRejected(any()))
        .thenReturn(List.of(nearbyVet, farVet));

    List<VetSummaryDto> results =
        vetDiscoveryService.searchNearbyVeterinarians(
            18.5204, 73.8567, 25.0, null, null, null);

    assertEquals(1, results.size());
    assertEquals("Dr. Nearby", results.get(0).name());
    assertEquals(VetMatchType.GPS_RADIUS, results.get(0).matchType());
  }
}
