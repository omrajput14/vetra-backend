package app.vetra.disease.geo;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

/**
 * Service managing real RFC 7946 compliant administrative boundary GeoJSON datasets.
 * Loads, validates, and caches State, District, and Taluka polygons in memory for sub-millisecond querying.
 */
@Service
public class AdministrativeBoundaryService {

  private static final Logger log = LoggerFactory.getLogger(AdministrativeBoundaryService.class);

  private final ResourceLoader resourceLoader;
  private final ObjectMapper objectMapper;

  private GeoJsonFeatureCollection stateBoundaries;
  private GeoJsonFeatureCollection districtBoundaries;
  private GeoJsonFeatureCollection talukaBoundaries;
  private GeoJsonFeatureCollection allBoundaries;
  private List<String> availableDistricts = Collections.emptyList();

  /**
   * Constructor injection.
   *
   * @param resourceLoader Spring resource loader
   * @param objectMapper Jackson JSON object mapper
   */
  public AdministrativeBoundaryService(ResourceLoader resourceLoader, ObjectMapper objectMapper) {
    this.resourceLoader = resourceLoader;
    this.objectMapper = objectMapper;
  }

  /**
   * Initializes, validates, and caches administrative boundaries from classpath resources on startup.
   */
  @PostConstruct
  public void init() {
    this.stateBoundaries = loadAndValidateFeatureCollection(
        "classpath:geo/boundaries/maharashtra_state.geojson", "STATE");
    this.districtBoundaries = loadAndValidateFeatureCollection(
        "classpath:geo/boundaries/maharashtra_districts.geojson", "DISTRICT");
    this.talukaBoundaries = loadAndValidateFeatureCollection(
        "classpath:geo/boundaries/maharashtra_talukas.geojson", "TALUKA");

    List<GeoJsonFeatureCollection.GeoJsonFeature> combined = combineFeatures(
        stateBoundaries, districtBoundaries, talukaBoundaries);
    this.allBoundaries = GeoJsonFeatureCollection.of(combined);
    this.availableDistricts = extractDistrictNames(districtBoundaries);

    log.info(
        "Administrative boundaries validated and initialized: {} state, {} districts, {} talukas (total: {})",
        getFeatureCount(stateBoundaries),
        getFeatureCount(districtBoundaries),
        getFeatureCount(talukaBoundaries),
        combined.size());
  }

  /**
   * Retrieves administrative boundaries filtered by administrative level, district, and state.
   *
   * @param level level filter: ALL, STATE / ADM1, DISTRICT / ADM2, TALUKA / ADM3
   * @param district optional district name filter (e.g. "Pune", "Satara")
   * @param state optional state name filter (default "Maharashtra")
   * @return {@link GeoJsonFeatureCollection}
   */
  public GeoJsonFeatureCollection getBoundaries(String level, String district, String state) {
    List<GeoJsonFeatureCollection.GeoJsonFeature> candidates = selectFeaturesByLevel(level);

    if (district == null || district.isBlank() || "ALL".equalsIgnoreCase(district.trim())) {
      return GeoJsonFeatureCollection.of(candidates);
    }

    String targetDistrict = district.trim().toLowerCase(Locale.ROOT);
    return GeoJsonFeatureCollection.of(filterByDistrict(candidates, targetDistrict));
  }

  /**
   * Returns list of available district names extracted dynamically from the GeoJSON dataset.
   *
   * @return list of district names
   */
  public List<String> getAvailableDistricts() {
    return availableDistricts;
  }

  private List<GeoJsonFeatureCollection.GeoJsonFeature> combineFeatures(GeoJsonFeatureCollection... collections) {
    List<GeoJsonFeatureCollection.GeoJsonFeature> list = new ArrayList<>();
    for (GeoJsonFeatureCollection coll : collections) {
      if (coll != null && coll.features() != null) {
        list.addAll(coll.features());
      }
    }
    return list;
  }

  private List<String> extractDistrictNames(GeoJsonFeatureCollection districtColl) {
    if (districtColl == null || districtColl.features() == null) {
      return Collections.emptyList();
    }
    return districtColl.features().stream()
        .map(GeoJsonFeatureCollection.GeoJsonFeature::properties)
        .filter(props -> props != null && props.containsKey("name"))
        .map(props -> props.get("name").toString())
        .sorted()
        .toList();
  }

  private int getFeatureCount(GeoJsonFeatureCollection coll) {
    return (coll != null && coll.features() != null) ? coll.features().size() : 0;
  }

  private List<GeoJsonFeatureCollection.GeoJsonFeature> selectFeaturesByLevel(String level) {
    String normalizedLevel = (level != null && !level.isBlank()) ? level.trim().toUpperCase(Locale.ROOT) : "ALL";

    switch (normalizedLevel) {
      case "STATE":
      case "ADM1":
        return stateBoundaries != null ? stateBoundaries.features() : Collections.emptyList();
      case "DISTRICT":
      case "ADM2":
        return districtBoundaries != null ? districtBoundaries.features() : Collections.emptyList();
      case "TALUKA":
      case "ADM3":
        return talukaBoundaries != null ? talukaBoundaries.features() : Collections.emptyList();
      case "ALL":
      default:
        return allBoundaries != null ? allBoundaries.features() : Collections.emptyList();
    }
  }

  private List<GeoJsonFeatureCollection.GeoJsonFeature> filterByDistrict(
      List<GeoJsonFeatureCollection.GeoJsonFeature> features, String targetDistrict) {
    if (features == null || features.isEmpty()) {
      return Collections.emptyList();
    }

    return features.stream()
        .filter(f -> matchesDistrict(f.properties(), targetDistrict))
        .toList();
  }

  private boolean matchesDistrict(Map<String, Object> props, String targetDistrict) {
    if (props == null) {
      return false;
    }
    Object distProp = props.get("district");
    Object nameProp = props.get("name");
    String distStr = distProp != null ? distProp.toString().toLowerCase(Locale.ROOT) : "";
    String nameStr = nameProp != null ? nameProp.toString().toLowerCase(Locale.ROOT) : "";
    return distStr.equalsIgnoreCase(targetDistrict) || nameStr.equalsIgnoreCase(targetDistrict);
  }

  private GeoJsonFeatureCollection loadAndValidateFeatureCollection(String resourcePath, String expectedLevel) {
    try {
      Resource resource = resourceLoader.getResource(resourcePath);
      if (!resource.exists()) {
        log.warn("GeoJSON resource not found at path: {}", resourcePath);
        return GeoJsonFeatureCollection.of(Collections.emptyList());
      }
      GeoJsonFeatureCollection collection;
      try (InputStream is = resource.getInputStream()) {
        collection = objectMapper.readValue(is, GeoJsonFeatureCollection.class);
      }

      if (collection == null || collection.features() == null) {
        log.error("Empty or invalid FeatureCollection loaded from {}", resourcePath);
        return GeoJsonFeatureCollection.of(Collections.emptyList());
      }

      validateFeatures(collection.features(), expectedLevel, resourcePath);
      return collection;
    } catch (Exception e) {
      log.error("Failed to load/validate GeoJSON resource at path {}: {}", resourcePath, e.getMessage(), e);
      return GeoJsonFeatureCollection.of(Collections.emptyList());
    }
  }

  private void validateFeatures(
      List<GeoJsonFeatureCollection.GeoJsonFeature> features, String expectedLevel, String resourcePath) {
    for (GeoJsonFeatureCollection.GeoJsonFeature f : features) {
      if (!"Feature".equals(f.type())) {
        throw new IllegalStateException("Invalid GeoJSON feature type: " + f.type());
      }
      validateGeometry(f.geometry(), resourcePath);
      validateProperties(f.properties(), expectedLevel, resourcePath);
    }
  }

  private void validateGeometry(GeoJsonFeatureCollection.GeoJsonGeometry geom, String resourcePath) {
    if (geom == null || geom.type() == null || geom.coordinates() == null) {
      throw new IllegalStateException("Feature missing valid geometry in " + resourcePath);
    }
    String gType = geom.type();
    if (!"Polygon".equals(gType) && !"MultiPolygon".equals(gType)) {
      throw new IllegalStateException("Unexpected geometry type for boundary: " + gType);
    }
  }

  private void validateProperties(Map<String, Object> props, String expectedLevel, String resourcePath) {
    if (props == null || !props.containsKey("name") || !props.containsKey("administrativeLevel")) {
      throw new IllegalStateException("Feature missing required properties in " + resourcePath);
    }
    if ("TALUKA".equals(expectedLevel) && (!props.containsKey("district") || props.get("district") == null)) {
      throw new IllegalStateException("TALUKA feature missing parent district association in " + resourcePath);
    }
  }
}
