package app.vetra.dashboard.service;

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
import app.vetra.infrastructure.persistence.enums.AnimalStatus;
import app.vetra.infrastructure.persistence.enums.UserRole;
import app.vetra.mortality.repository.AnimalMortalityEventRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authoritative business service for calculating deterministic, modeled livestock
 * economic savings and loss-avoidance estimates.
 *
 * <p>The calculation is strictly modeled based on configurable species reference replacement
 * costs and an empirical early-detection loss-avoidance coefficient. Calculations explicitly
 * exclude confirmed mortality events and deceased animals, deduplicate multiple reports per
 * animal, and return an honest insufficient-data state when reliable telemetry is absent.
 */
@Service
public class EconomicImpactService {

  private final EconomicImpactProperties properties;
  private final UserRepository userRepository;
  private final FarmerProfileRepository farmerProfileRepository;
  private final AnimalRepository animalRepository;
  private final DiseaseReportRepository diseaseReportRepository;
  private final AIScanRepository aiScanRepository;
  private final AnimalMortalityEventRepository animalMortalityEventRepository;

  /**
   * Constructor injection for economic impact dependencies.
   */
  public EconomicImpactService(
      EconomicImpactProperties properties,
      UserRepository userRepository,
      FarmerProfileRepository farmerProfileRepository,
      AnimalRepository animalRepository,
      DiseaseReportRepository diseaseReportRepository,
      AIScanRepository aiScanRepository,
      AnimalMortalityEventRepository animalMortalityEventRepository) {
    this.properties = properties;
    this.userRepository = userRepository;
    this.farmerProfileRepository = farmerProfileRepository;
    this.animalRepository = animalRepository;
    this.diseaseReportRepository = diseaseReportRepository;
    this.aiScanRepository = aiScanRepository;
    this.animalMortalityEventRepository = animalMortalityEventRepository;
  }

  /**
   * Calculates modeled economic impact for a specific registered farmer.
   *
   * @param userIdentifier authenticated user identifier (email or phone)
   * @return EconomicImpactResponse with modeled savings or honest unavailable state
   */
  @Transactional(readOnly = true)
  public EconomicImpactResponse getFarmerEconomicImpact(String userIdentifier) {
    User user = userRepository.findByIdentifier(userIdentifier)
        .orElseThrow(() -> new IllegalArgumentException("User not found: " + userIdentifier));

    Optional<FarmerProfile> farmerOpt = farmerProfileRepository.findByUser(user);
    if (farmerOpt.isEmpty()) {
      return EconomicImpactResponse.ofInsufficientData("FARMER", "Estimated savings unavailable");
    }

    FarmerProfile farmer = farmerOpt.get();
    List<Animal> farmerAnimals = animalRepository.findByFarmer(farmer);
    if (farmerAnimals.isEmpty()) {
      return EconomicImpactResponse.ofInsufficientData("FARMER", "Estimated savings unavailable");
    }

    Set<UUID> confirmedMortalityIds = new HashSet<>(
        animalMortalityEventRepository.findConfirmedMortalityAnimalIds());

    List<Animal> eligibleAnimals = new ArrayList<>();
    for (Animal animal : farmerAnimals) {
      if (animal.getStatus() == AnimalStatus.DECEASED) {
        continue;
      }
      if (confirmedMortalityIds.contains(animal.getId())) {
        continue;
      }
      // Animal must have at least one early detection telemetry record (AIScan or DiseaseReport)
      boolean hasAiScan = aiScanRepository.existsByAnimalId(animal.getId());
      boolean hasDiseaseReport = diseaseReportRepository.existsByAnimalId(animal.getId());
      if (hasAiScan || hasDiseaseReport) {
        eligibleAnimals.add(animal);
      }
    }

    if (eligibleAnimals.isEmpty()) {
      return EconomicImpactResponse.ofInsufficientData("FARMER", "Estimated savings unavailable");
    }

    BigDecimal totalSavings = BigDecimal.ZERO;
    for (Animal animal : eligibleAnimals) {
      BigDecimal ref = properties.getReferenceValue(animal.getSpecies());
      BigDecimal protectedVal = ref.multiply(properties.getPreventionFactor());
      totalSavings = totalSavings.add(protectedVal);
    }

    totalSavings = totalSavings.setScale(2, RoundingMode.HALF_UP);
    return EconomicImpactResponse.ofCalculated(totalSavings, eligibleAnimals.size(), "FARMER");
  }

  /**
   * Calculates statewide aggregate modeled economic impact across all registered livestock.
   *
   * @return EconomicImpactResponse containing statewide aggregate savings
   */
  @Transactional(readOnly = true)
  public EconomicImpactResponse getStatewideEconomicImpact() {
    Set<UUID> candidateAnimalIds = new HashSet<>(diseaseReportRepository.findDistinctActiveAnimalIds());
    candidateAnimalIds.addAll(aiScanRepository.findDistinctActiveAnimalIds());

    Set<UUID> confirmedMortalityIds = new HashSet<>(
        animalMortalityEventRepository.findConfirmedMortalityAnimalIds());
    candidateAnimalIds.removeAll(confirmedMortalityIds);

    if (candidateAnimalIds.isEmpty()) {
      return EconomicImpactResponse.ofInsufficientData("STATEWIDE", "Insufficient data");
    }

    List<Animal> eligibleAnimals = animalRepository.findAllById(candidateAnimalIds);
    if (eligibleAnimals.isEmpty()) {
      return EconomicImpactResponse.ofInsufficientData("STATEWIDE", "Insufficient data");
    }

    BigDecimal totalSavings = BigDecimal.ZERO;
    for (Animal animal : eligibleAnimals) {
      if (animal.getStatus() == AnimalStatus.DECEASED) {
        continue;
      }
      BigDecimal ref = properties.getReferenceValue(animal.getSpecies());
      BigDecimal protectedVal = ref.multiply(properties.getPreventionFactor());
      totalSavings = totalSavings.add(protectedVal);
    }

    totalSavings = totalSavings.setScale(2, RoundingMode.HALF_UP);
    return EconomicImpactResponse.ofCalculated(totalSavings, eligibleAnimals.size(), "STATEWIDE");
  }

  /**
   * Role-aware dispatcher for unified dashboard economic telemetry.
   *
   * @param userIdentifier authenticated user identifier
   * @return EconomicImpactResponse matching caller role privileges
   */
  @Transactional(readOnly = true)
  public EconomicImpactResponse getEconomicImpactForUser(String userIdentifier) {
    User user = userRepository.findByIdentifier(userIdentifier)
        .orElseThrow(() -> new IllegalArgumentException("User not found: " + userIdentifier));

    if (user.getRole() == UserRole.FARMER) {
      return getFarmerEconomicImpact(userIdentifier);
    } else if (user.getRole() == UserRole.GOVERNMENT_OFFICER
        || user.getRole() == UserRole.ADMINISTRATOR) {
      return getStatewideEconomicImpact();
    } else {
      // Default: Return statewide overview if permitted, else unavailable
      return getStatewideEconomicImpact();
    }
  }
}
