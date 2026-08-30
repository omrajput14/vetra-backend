package app.vetra.disease.risk.vaccination;

import app.vetra.animal.repository.AnimalHealthRecordRepository;
import app.vetra.animal.repository.AnimalRepository;
import app.vetra.disease.geo.GeoUtils;
import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.entity.AnimalHealthRecord;
import app.vetra.infrastructure.persistence.enums.HealthRecordType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service evaluating livestock vaccination coverage and calculating regional immunity gaps.
 * Uses real animal registry data and verifies active, non-expired vaccinations against the target pathogen.
 */
@Service
public class VaccinationGapService {

  private final AnimalRepository animalRepository;
  private final AnimalHealthRecordRepository healthRecordRepository;

  /** Constructor injection. */
  public VaccinationGapService(
      AnimalRepository animalRepository, AnimalHealthRecordRepository healthRecordRepository) {
    this.animalRepository = animalRepository;
    this.healthRecordRepository = healthRecordRepository;
  }

  /**
   * Calculates herd vaccination coverage and the resulting immunity gap within a geographic radius.
   *
   * @param diseaseName target disease/pathogen
   * @param centerLatitude cluster center latitude
   * @param centerLongitude cluster center longitude
   * @param radiusKm geographic search radius in kilometers
   * @return {@link VaccinationGapResult}
   */
  @Transactional(readOnly = true)
  public VaccinationGapResult calculateVaccinationGap(
      String diseaseName, double centerLatitude, double centerLongitude, double radiusKm) {
    List<Animal> localAnimals = findAnimalsInRadius(centerLatitude, centerLongitude, radiusKm);
    int totalAnimals = localAnimals.size();

    if (totalAnimals == 0) {
      return new VaccinationGapResult(
          55.0,
          0,
          0,
          45.0,
          55.0,
          "No localized livestock registry available in immediate sector; standard regional baseline gap applied.");
    }

    LocalDateTime oneYearAgo = LocalDateTime.now().minusDays(365);
    LocalDate today = LocalDate.now();

    long vaccinatedCount =
        localAnimals.stream()
            .filter(animal -> isAnimalVaccinated(animal, diseaseName, oneYearAgo, today))
            .count();

    double coveragePct = ((double) vaccinatedCount / totalAnimals) * 100.0;
    double gapPct = Math.max(0.0, 100.0 - coveragePct);
    double normalizedScore = Math.min(100.0, Math.max(0.0, gapPct));

    String explanation =
        String.format(
            "%.1f%% vaccination coverage across %d registered livestock in %.1f km perimeter (%.1f%% immunity gap)",
            coveragePct,
            totalAnimals,
            radiusKm,
            gapPct);

    return new VaccinationGapResult(
        normalizedScore, totalAnimals, (int) vaccinatedCount, coveragePct, gapPct, explanation);
  }

  private List<Animal> findAnimalsInRadius(double centerLat, double centerLng, double radiusKm) {
    return animalRepository.findAll().stream()
        .filter(
            a ->
                a.getFarmer() != null
                    && a.getFarmer().getLatitude() != null
                    && a.getFarmer().getLongitude() != null)
        .filter(
            a ->
                GeoUtils.calculateDistanceKm(
                        centerLat,
                        centerLng,
                        a.getFarmer().getLatitude(),
                        a.getFarmer().getLongitude())
                    <= radiusKm)
        .toList();
  }

  private boolean isAnimalVaccinated(
      Animal animal, String diseaseName, LocalDateTime oneYearAgo, LocalDate today) {
    List<AnimalHealthRecord> records =
        healthRecordRepository.findByAnimalOrderByRecordedAtDesc(animal);
    return records.stream()
        .anyMatch(
            r ->
                r.getRecordType() == HealthRecordType.VACCINATION
                    && isMatchingVaccine(diseaseName, r)
                    && isVaccineValid(r, oneYearAgo, today));
  }

  private boolean isMatchingVaccine(String diseaseName, AnimalHealthRecord record) {
    if (diseaseName == null) {
      return true;
    }
    String d = diseaseName.toLowerCase();
    String text =
        (record.getVaccineName() + " " + record.getTitle() + " " + record.getDescription())
            .toLowerCase();

    if (d.contains("fmd") || d.contains("foot")) {
      return text.contains("fmd") || text.contains("foot");
    }
    if (d.contains("lsd") || d.contains("lumpy")) {
      return text.contains("lsd") || text.contains("lumpy");
    }
    if (d.contains("rabies")) {
      return text.contains("rabies");
    }
    if (d.contains("brucell")) {
      return text.contains("brucell");
    }
    if (d.contains("anthrax")) {
      return text.contains("anthrax");
    }

    return true;
  }

  private boolean isVaccineValid(
      AnimalHealthRecord record, LocalDateTime oneYearAgo, LocalDate today) {
    if (record.getNextDueDate() != null && record.getNextDueDate().isAfter(today)) {
      return true;
    }
    return record.getRecordedAt() != null && record.getRecordedAt().isAfter(oneYearAgo);
  }

  /** Result record of vaccination gap assessment. */
  public record VaccinationGapResult(
      double normalizedScore,
      int totalEligibleAnimals,
      int vaccinatedAnimals,
      double coveragePercentage,
      double gapPercentage,
      String explanation) {}
}
