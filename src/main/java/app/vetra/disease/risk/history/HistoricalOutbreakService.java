package app.vetra.disease.risk.history;

import app.vetra.disease.entity.Outbreak;
import app.vetra.disease.geo.GeoUtils;
import app.vetra.disease.repository.OutbreakRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Historical Outbreak Analysis Service. Evaluates 24-month regional recurrence and Indian
 * agro-climatic seasonal epidemic vulnerability.
 */
@Service
public class HistoricalOutbreakService {

  private final OutbreakRepository outbreakRepository;

  /** Constructor injection. */
  public HistoricalOutbreakService(OutbreakRepository outbreakRepository) {
    this.outbreakRepository = outbreakRepository;
  }

  /**
   * Evaluates historical recurrence and seasonal surge indicators for a disease cluster.
   *
   * @param diseaseName name of disease
   * @param latitude cluster center latitude
   * @param longitude cluster center longitude
   * @param radiusKm search radius in kilometers
   * @return {@link HistoricalSignalResult}
   */
  @Transactional(readOnly = true)
  public HistoricalSignalResult evaluateHistory(
      String diseaseName, double latitude, double longitude, double radiusKm) {
    Instant cutoff = Instant.now().minus(730, ChronoUnit.DAYS); // 24 months
    double searchRadius = Math.max(25.0, radiusKm * 2.0);

    List<Outbreak> pastOutbreaks =
        outbreakRepository.findByDiseaseNameIgnoreCase(diseaseName, null).getContent().stream()
            .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(cutoff))
            .filter(
                o ->
                    GeoUtils.calculateDistanceKm(
                            latitude, longitude, o.getCenterLatitude(), o.getCenterLongitude())
                        <= searchRadius)
            .toList();

    int pastRecurrenceCount = pastOutbreaks.size();
    boolean isSeasonalPeak = isSeasonalEpidemicMonth(diseaseName);

    // Normalization: each past outbreak contributes 25 points, seasonal surge contributes 30 points
    double rawScore = (pastRecurrenceCount * 25.0) + (isSeasonalPeak ? 30.0 : 10.0);
    double normalizedScore = Math.min(100.0, Math.max(0.0, rawScore));

    String description =
        String.format(
            "%d historical outbreak(s) in %.0f km radius over 24 months; %s",
            pastRecurrenceCount,
            searchRadius,
            isSeasonalPeak
                ? "Active seasonal epidemic window for " + diseaseName
                : "Standard non-peak seasonal window");

    return new HistoricalSignalResult(
        normalizedScore, pastRecurrenceCount, isSeasonalPeak, description);
  }

  private boolean isSeasonalEpidemicMonth(String diseaseName) {
    if (diseaseName == null) {
      return false;
    }
    int currentMonth = LocalDate.now(ZoneId.systemDefault()).getMonthValue();
    String key = diseaseName.toLowerCase();

    if (key.contains("foot") || key.contains("fmd")) {
      // Monsoon & post-monsoon transmission: June(6) to October(10)
      return Set.of(6, 7, 8, 9, 10).contains(currentMonth);
    } else if (key.contains("lumpy") || key.contains("lsd")) {
      // Vector-borne transmission peak: August(8) to November(11)
      return Set.of(8, 9, 10, 11).contains(currentMonth);
    } else if (key.contains("anthrax")) {
      // Post-drought soil rain exposure: May(5) to July(7)
      return Set.of(5, 6, 7).contains(currentMonth);
    } else if (key.contains("brucellosis")) {
      // Peak calving season: January(1) to April(4)
      return Set.of(1, 2, 3, 4).contains(currentMonth);
    } else if (key.contains("hemorrhagic") || key.contains("blackleg") || key.contains("bq") || key.contains("hs")) {
      // Early monsoon spore surge: June(6) to August(8)
      return Set.of(6, 7, 8).contains(currentMonth);
    }

    return false;
  }

  /** Result record of historical signal evaluation. */
  public record HistoricalSignalResult(
      double normalizedScore,
      int pastOutbreaksCount,
      boolean seasonalPeak,
      String explanation) {}
}
