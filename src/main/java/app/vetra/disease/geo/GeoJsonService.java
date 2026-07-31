package app.vetra.disease.geo;

import app.vetra.disease.entity.Outbreak;
import app.vetra.disease.entity.OutbreakStatus;
import app.vetra.disease.repository.DiseaseReportRepository;
import app.vetra.disease.repository.OutbreakRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service generating RFC 7946 compliant GeoJSON FeatureCollections and spatial heatmap datasets.
 */
@Service
public class GeoJsonService {

  private final OutbreakRepository outbreakRepository;
  private final DiseaseReportRepository diseaseReportRepository;

  /** Constructor injection. */
  public GeoJsonService(OutbreakRepository outbreakRepository, DiseaseReportRepository diseaseReportRepository) {
    this.outbreakRepository = outbreakRepository;
    this.diseaseReportRepository = diseaseReportRepository;
  }

  /**
   * Exports active outbreak clusters as a valid GeoJSON FeatureCollection.
   *
   * @return {@link GeoJsonFeatureCollection}
   */
  @Transactional(readOnly = true)
  public GeoJsonFeatureCollection getOutbreaksGeoJson() {
    List<Outbreak> activeOutbreaks = outbreakRepository.findAll()
        .stream()
        .filter(o -> o.getStatus() != OutbreakStatus.RESOLVED)
        .toList();

    List<GeoJsonFeatureCollection.GeoJsonFeature> features = activeOutbreaks.stream()
        .map(o -> {
          Map<String, Object> props = new HashMap<>();
          props.put("id", o.getId().toString());
          props.put("diseaseName", o.getDiseaseName());
          props.put("severity", o.getSeverity());
          props.put("status", o.getStatus().name());
          props.put("riskScore", o.getRiskScore().name());
          props.put("trend", o.getTrend().name());
          props.put("radiusKm", o.getRadiusKm());
          props.put("affectedReportsCount", o.getAffectedReportsCount());

          return GeoJsonFeatureCollection.GeoJsonFeature.point(
              o.getCenterLongitude(), o.getCenterLatitude(), props);
        })
        .toList();

    return GeoJsonFeatureCollection.of(features);
  }

  /**
   * Computes spatial heatmap hotspot dataset with normalized intensity weights (0.0 to 1.0).
   *
   * @return list of {@link HeatmapPoint}
   */
  @Transactional(readOnly = true)
  public List<HeatmapPoint> getHeatmapData() {
    List<Outbreak> active = outbreakRepository.findAll()
        .stream()
        .filter(o -> o.getStatus() != OutbreakStatus.RESOLVED)
        .toList();

    int maxCases = active.stream().mapToInt(Outbreak::getAffectedReportsCount).max().orElse(1);

    return active.stream()
        .map(o -> {
          double intensity = Math.min(1.0, (double) o.getAffectedReportsCount() / Math.max(1, maxCases));
          return new HeatmapPoint(
              o.getCenterLatitude(),
              o.getCenterLongitude(),
              Math.round(intensity * 100.0) / 100.0,
              o.getAffectedReportsCount(),
              o.getDiseaseName()
          );
        })
        .toList();
  }
}
