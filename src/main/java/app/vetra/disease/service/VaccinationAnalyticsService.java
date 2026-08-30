package app.vetra.disease.service;

import app.vetra.animal.repository.AnimalHealthRecordRepository;
import app.vetra.animal.repository.AnimalRepository;
import app.vetra.disease.dto.VaccinationAnalyticsResponse;
import app.vetra.disease.dto.VaccinationAnalyticsResponse.PathogenCoverageDto;
import app.vetra.disease.dto.VaccinationAnalyticsResponse.PriorityImmunityDeficitZoneDto;
import app.vetra.disease.dto.VaccinationAnalyticsResponse.ZoneVaccinationGapDto;
import app.vetra.disease.entity.Outbreak;
import app.vetra.disease.entity.OutbreakStatus;
import app.vetra.disease.repository.OutbreakRepository;
import app.vetra.disease.risk.vaccination.VaccinationGapService;
import app.vetra.disease.risk.vaccination.VaccinationGapService.VaccinationGapResult;
import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.entity.AnimalHealthRecord;
import app.vetra.infrastructure.persistence.enums.HealthRecordType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service aggregating livestock vaccination coverage, pathogen immunization status,
 * and spatial immunity gap correlations.
 */
@Service
public class VaccinationAnalyticsService {

  private final AnimalRepository animalRepository;
  private final AnimalHealthRecordRepository healthRecordRepository;
  private final OutbreakRepository outbreakRepository;
  private final VaccinationGapService vaccinationGapService;

  private static final List<String> MONITORED_PATHOGENS =
      List.of(
          "Foot and Mouth Disease (FMD)",
          "Lumpy Skin Disease (LSD)",
          "Brucellosis",
          "Rabies",
          "Anthrax",
          "Blackleg",
          "Peste des Petits Ruminants (PPR)");

  /** Constructor injection. */
  public VaccinationAnalyticsService(
      AnimalRepository animalRepository,
      AnimalHealthRecordRepository healthRecordRepository,
      OutbreakRepository outbreakRepository,
      VaccinationGapService vaccinationGapService) {
    this.animalRepository = animalRepository;
    this.healthRecordRepository = healthRecordRepository;
    this.outbreakRepository = outbreakRepository;
    this.vaccinationGapService = vaccinationGapService;
  }

  /**
   * Generates comprehensive vaccination analytics and regional immunity gap intelligence.
   *
   * @return {@link VaccinationAnalyticsResponse}
   */
  @Transactional(readOnly = true)
  public VaccinationAnalyticsResponse getVaccinationAnalytics() {
    List<Animal> allAnimals = animalRepository.findAll();
    long totalAnimals = allAnimals.size();

    if (totalAnimals == 0) {
      return new VaccinationAnalyticsResponse(
          0, 0, 0, 0.0, 100.0, List.of(), List.of(), List.of());
    }

    LocalDateTime oneYearAgo = LocalDateTime.now().minusDays(365);
    LocalDate today = LocalDate.now();

    List<AnimalHealthRecord> validVaccinations =
        healthRecordRepository.findAll().stream()
            .filter(r -> r.getRecordType() == HealthRecordType.VACCINATION)
            .filter(r -> isRecordValid(r, oneYearAgo, today))
            .toList();

    Set<UUID> vaccinatedAnimalIds =
        validVaccinations.stream()
            .map(r -> r.getAnimal().getId())
            .collect(Collectors.toSet());

    long totalVaccinated = vaccinatedAnimalIds.size();
    long totalUnvaccinated = Math.max(0, totalAnimals - totalVaccinated);
    double overallCoveragePct = ((double) totalVaccinated / totalAnimals) * 100.0;
    double overallImmunityGapPct = Math.max(0.0, 100.0 - overallCoveragePct);

    List<PathogenCoverageDto> pathogenCoverageList =
        buildPathogenCoverage(allAnimals, validVaccinations, totalAnimals);

    List<ZoneVaccinationGapDto> zoneGaps = new ArrayList<>();
    List<PriorityImmunityDeficitZoneDto> priorityZones = new ArrayList<>();
    buildZoneAndPriorityGaps(zoneGaps, priorityZones);

    return new VaccinationAnalyticsResponse(
        totalAnimals,
        totalVaccinated,
        totalUnvaccinated,
        Math.round(overallCoveragePct * 10.0) / 10.0,
        Math.round(overallImmunityGapPct * 10.0) / 10.0,
        pathogenCoverageList,
        zoneGaps,
        priorityZones);
  }

  private List<PathogenCoverageDto> buildPathogenCoverage(
      List<Animal> allAnimals, List<AnimalHealthRecord> validVaccinations, long totalAnimals) {
    List<PathogenCoverageDto> list = new ArrayList<>();
    for (String pathogen : MONITORED_PATHOGENS) {
      long vaccinatedCount =
          allAnimals.stream()
              .filter(a -> hasValidVaccinationForPathogen(a, pathogen, validVaccinations))
              .count();

      double coverage = ((double) vaccinatedCount / totalAnimals) * 100.0;
      double gap = Math.max(0.0, 100.0 - coverage);
      double target = 80.0;

      String status;
      if (coverage >= target) {
        status = "ADEQUATE";
      } else if (coverage >= 50.0) {
        status = "DEFICIT";
      } else {
        status = "CRITICAL_GAP";
      }

      list.add(
          new PathogenCoverageDto(
              pathogen,
              vaccinatedCount,
              totalAnimals,
              Math.round(coverage * 10.0) / 10.0,
              Math.round(gap * 10.0) / 10.0,
              target,
              status));
    }
    return list;
  }

  private void buildZoneAndPriorityGaps(
      List<ZoneVaccinationGapDto> zoneGaps,
      List<PriorityImmunityDeficitZoneDto> priorityZones) {
    List<Outbreak> activeOutbreaks =
        outbreakRepository.findAll().stream()
            .filter(o -> o.getStatus() != OutbreakStatus.RESOLVED)
            .toList();

    for (Outbreak outbreak : activeOutbreaks) {
      VaccinationGapResult gapResult =
          vaccinationGapService.calculateVaccinationGap(
              outbreak.getDiseaseName(),
              outbreak.getCenterLatitude(),
              outbreak.getCenterLongitude(),
              outbreak.getRadiusKm());

      String zoneName =
          String.format(
              "%s Zone (Lat %.2f, Lng %.2f)",
              outbreak.getDiseaseName(),
              outbreak.getCenterLatitude(),
              outbreak.getCenterLongitude());

      zoneGaps.add(
          new ZoneVaccinationGapDto(
              outbreak.getId(),
              zoneName,
              outbreak.getDiseaseName(),
              outbreak.getCenterLatitude(),
              outbreak.getCenterLongitude(),
              outbreak.getRadiusKm(),
              gapResult.totalEligibleAnimals(),
              gapResult.vaccinatedAnimals(),
              Math.round(gapResult.coveragePercentage() * 10.0) / 10.0,
              Math.round(gapResult.gapPercentage() * 10.0) / 10.0,
              outbreak.getRiskScore() != null ? outbreak.getRiskScore().name() : "HIGH"));

      double riskScore =
          outbreak.getCompositeRiskScore() != null
              ? outbreak.getCompositeRiskScore().doubleValue()
              : 50.0;
      double immunityGap = gapResult.gapPercentage();

      String priority;
      String action;
      if (riskScore >= 75.0 || immunityGap >= 60.0) {
        priority = "URGENT_RING_VACCINATION";
        action =
            String.format(
                "Deploy emergency ring vaccination within %.0f km buffer; dispatch mobile veterinary units.",
                outbreak.getRadiusKm());
      } else if (riskScore >= 45.0 || immunityGap >= 40.0) {
        priority = "ELEVATED_SURVEILLANCE";
        action = "Intensify daily block-level clinical surveillance and audit vaccination records.";
      } else {
        priority = "ROUTINE_MONITORING";
        action = "Maintain scheduled periodic immunization schedule.";
      }

      priorityZones.add(
          new PriorityImmunityDeficitZoneDto(
              outbreak.getId(),
              zoneName,
              outbreak.getDiseaseName(),
              Math.round(immunityGap * 10.0) / 10.0,
              Math.round(riskScore * 10.0) / 10.0,
              priority,
              action));
    }

    priorityZones.sort(
        Comparator.comparingDouble(
                (PriorityImmunityDeficitZoneDto p) -> p.outbreakRiskScore() + p.immunityGapPercentage())
            .reversed());
  }

  private boolean hasValidVaccinationForPathogen(
      Animal animal, String pathogen, List<AnimalHealthRecord> allVaccinations) {
    return allVaccinations.stream()
        .filter(r -> r.getAnimal().getId().equals(animal.getId()))
        .anyMatch(r -> matchesPathogen(pathogen, r));
  }

  private boolean matchesPathogen(String pathogen, AnimalHealthRecord record) {
    String p = pathogen.toLowerCase();
    String text =
        (record.getVaccineName() + " " + record.getTitle() + " " + record.getDescription())
            .toLowerCase();

    if (p.contains("fmd") || p.contains("foot")) {
      return text.contains("fmd") || text.contains("foot");
    }
    if (p.contains("lsd") || p.contains("lumpy")) {
      return text.contains("lsd") || text.contains("lumpy");
    }
    if (p.contains("rabies")) {
      return text.contains("rabies");
    }
    if (p.contains("brucell")) {
      return text.contains("brucell");
    }
    if (p.contains("anthrax")) {
      return text.contains("anthrax");
    }
    if (p.contains("blackleg")) {
      return text.contains("blackleg") || text.contains("bq");
    }
    if (p.contains("ppr") || p.contains("petits")) {
      return text.contains("ppr") || text.contains("pest");
    }

    return text.contains(p);
  }

  private boolean isRecordValid(
      AnimalHealthRecord record, LocalDateTime oneYearAgo, LocalDate today) {
    if (record.getNextDueDate() != null && record.getNextDueDate().isAfter(today)) {
      return true;
    }
    return record.getRecordedAt() != null && record.getRecordedAt().isAfter(oneYearAgo);
  }
}
