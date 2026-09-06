package app.vetra.dashboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import app.vetra.ai.repository.AIScanRepository;
import app.vetra.animal.repository.AnimalRepository;
import app.vetra.auth.repository.FarmerProfileRepository;
import app.vetra.auth.repository.UserRepository;
import app.vetra.dashboard.config.EconomicImpactProperties;
import app.vetra.dashboard.dto.EconomicImpactResponse;
import app.vetra.disease.repository.DiseaseReportRepository;
import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.entity.FarmerProfile;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.enums.AnimalGender;
import app.vetra.infrastructure.persistence.enums.AnimalStatus;
import app.vetra.infrastructure.persistence.enums.Species;
import app.vetra.infrastructure.persistence.enums.UserRole;
import app.vetra.mortality.repository.AnimalMortalityEventRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EconomicImpactServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private FarmerProfileRepository farmerProfileRepository;

  @Mock
  private AnimalRepository animalRepository;

  @Mock
  private DiseaseReportRepository diseaseReportRepository;

  @Mock
  private AIScanRepository aiScanRepository;

  @Mock
  private AnimalMortalityEventRepository animalMortalityEventRepository;

  private EconomicImpactProperties properties;
  private EconomicImpactService service;

  private User testUser;
  private FarmerProfile testFarmer;

  @BeforeEach
  void setUp() {
    properties = new EconomicImpactProperties();
    // Default factor is 0.40, cattle: 45000, goat: 8000
    service = new EconomicImpactService(
        properties,
        userRepository,
        farmerProfileRepository,
        animalRepository,
        diseaseReportRepository,
        aiScanRepository,
        animalMortalityEventRepository);

    testUser = User.builder()
        .email("farmer@vetra.co.in")
        .role(UserRole.FARMER)
        .build();
    testUser.setId(UUID.randomUUID());

    testFarmer = FarmerProfile.builder()
        .user(testUser)
        .fullName("Ramesh Patel")
        .build();
    testFarmer.setId(UUID.randomUUID());
  }

  @Test
  void testFarmerEconomicImpact_DeterministicCalculation() {
    UUID cowId = UUID.randomUUID();
    Animal cow = Animal.builder()
        .farmer(testFarmer)
        .species(Species.CATTLE)
        .tagNumber("IND-COW-01")
        .gender(AnimalGender.FEMALE)
        .status(AnimalStatus.ACTIVE)
        .build();
    cow.setId(cowId);

    UUID goatId = UUID.randomUUID();
    Animal goat = Animal.builder()
        .farmer(testFarmer)
        .species(Species.GOAT)
        .tagNumber("IND-GOAT-01")
        .gender(AnimalGender.MALE)
        .status(AnimalStatus.ACTIVE)
        .build();
    goat.setId(goatId);

    when(userRepository.findByIdentifier("farmer@vetra.co.in")).thenReturn(Optional.of(testUser));
    when(farmerProfileRepository.findByUser(testUser)).thenReturn(Optional.of(testFarmer));
    when(animalRepository.findByFarmer(testFarmer)).thenReturn(List.of(cow, goat));
    when(animalMortalityEventRepository.findConfirmedMortalityAnimalIds()).thenReturn(List.of());

    // Both have early detection telemetry
    when(aiScanRepository.existsByAnimalId(cowId)).thenReturn(true);
    when(diseaseReportRepository.existsByAnimalId(cowId)).thenReturn(false);
    when(aiScanRepository.existsByAnimalId(goatId)).thenReturn(false);
    when(diseaseReportRepository.existsByAnimalId(goatId)).thenReturn(true);

    EconomicImpactResponse response = service.getFarmerEconomicImpact("farmer@vetra.co.in");

    assertNotNull(response);
    assertTrue(response.isModeled());
    assertTrue(response.hasSufficientData());
    assertEquals(2, response.eligibleAnimalsCount());
    // 45000 * 0.40 = 18000; 8000 * 0.40 = 3200; total = 21200.00
    assertEquals(new BigDecimal("21200.00"), response.modeledSavings());
    assertEquals("₹21,200", response.formattedValue());
    assertEquals("Modeled estimate", response.label());
  }

  @Test
  void testFarmerEconomicImpact_InsufficientData_NoEarlyTelemetry() {
    UUID cowId = UUID.randomUUID();
    Animal cow = Animal.builder()
        .farmer(testFarmer)
        .species(Species.CATTLE)
        .tagNumber("IND-COW-02")
        .status(AnimalStatus.ACTIVE)
        .build();
    cow.setId(cowId);

    when(userRepository.findByIdentifier("farmer@vetra.co.in")).thenReturn(Optional.of(testUser));
    when(farmerProfileRepository.findByUser(testUser)).thenReturn(Optional.of(testFarmer));
    when(animalRepository.findByFarmer(testFarmer)).thenReturn(List.of(cow));
    when(animalMortalityEventRepository.findConfirmedMortalityAnimalIds()).thenReturn(List.of());

    // No early detection scans or reports
    when(aiScanRepository.existsByAnimalId(cowId)).thenReturn(false);
    when(diseaseReportRepository.existsByAnimalId(cowId)).thenReturn(false);

    EconomicImpactResponse response = service.getFarmerEconomicImpact("farmer@vetra.co.in");

    assertNotNull(response);
    assertFalse(response.hasSufficientData());
    assertNull(response.modeledSavings());
    assertNull(response.formattedValue());
    assertEquals("Estimated savings unavailable", response.statusMessage());
  }

  @Test
  void testFarmerEconomicImpact_ConfirmedMortalityExcluded() {
    UUID cowId = UUID.randomUUID();
    Animal cow = Animal.builder()
        .farmer(testFarmer)
        .species(Species.CATTLE)
        .tagNumber("IND-COW-03")
        .status(AnimalStatus.ACTIVE)
        .build();
    cow.setId(cowId);

    when(userRepository.findByIdentifier("farmer@vetra.co.in")).thenReturn(Optional.of(testUser));
    when(farmerProfileRepository.findByUser(testUser)).thenReturn(Optional.of(testFarmer));
    when(animalRepository.findByFarmer(testFarmer)).thenReturn(List.of(cow));
    // Cow has confirmed mortality in repository
    when(animalMortalityEventRepository.findConfirmedMortalityAnimalIds()).thenReturn(List.of(cowId));

    EconomicImpactResponse response = service.getFarmerEconomicImpact("farmer@vetra.co.in");

    assertNotNull(response);
    assertFalse(response.hasSufficientData());
    assertNull(response.modeledSavings());
  }

  @Test
  void testStatewideEconomicImpact_DeduplicationAndCalculation() {
    UUID animalId1 = UUID.randomUUID();
    UUID animalId2 = UUID.randomUUID();

    Animal buffalo = Animal.builder()
        .species(Species.BUFFALO)
        .tagNumber("IND-BUF-01")
        .status(AnimalStatus.ACTIVE)
        .build();
    buffalo.setId(animalId1);

    Animal sheep = Animal.builder()
        .species(Species.SHEEP)
        .tagNumber("IND-SHP-01")
        .status(AnimalStatus.ACTIVE)
        .build();
    sheep.setId(animalId2);

    // Overlapping candidates from scans and disease reports
    when(diseaseReportRepository.findDistinctActiveAnimalIds()).thenReturn(List.of(animalId1, animalId2));
    when(aiScanRepository.findDistinctActiveAnimalIds()).thenReturn(List.of(animalId1));
    when(animalMortalityEventRepository.findConfirmedMortalityAnimalIds()).thenReturn(List.of());
    when(animalRepository.findAllById(org.mockito.ArgumentMatchers.anySet())).thenReturn(List.of(buffalo, sheep));

    EconomicImpactResponse response = service.getStatewideEconomicImpact();

    assertNotNull(response);
    assertTrue(response.isModeled());
    assertTrue(response.hasSufficientData());
    assertEquals(2, response.eligibleAnimalsCount());
    // Buffalo (55000 * 0.40 = 22000) + Sheep (7000 * 0.40 = 2800) = 24800.00
    assertEquals(new BigDecimal("24800.00"), response.modeledSavings());
    assertEquals("₹24,800", response.formattedValue());
    assertEquals("STATEWIDE", response.scope());
  }

  @Test
  void testStatewideEconomicImpact_ZeroDataReturnsInsufficient() {
    when(diseaseReportRepository.findDistinctActiveAnimalIds()).thenReturn(List.of());
    when(aiScanRepository.findDistinctActiveAnimalIds()).thenReturn(List.of());
    when(animalMortalityEventRepository.findConfirmedMortalityAnimalIds()).thenReturn(List.of());

    EconomicImpactResponse response = service.getStatewideEconomicImpact();

    assertNotNull(response);
    assertFalse(response.hasSufficientData());
    assertNull(response.modeledSavings());
    assertEquals("Insufficient data", response.statusMessage());
  }
}
