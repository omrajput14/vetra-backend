package app.vetra.disease.geo;

import java.util.List;
import java.util.Map;

/**
 * RFC 7946 compliant GeoJSON FeatureCollection wrapper.
 */
public record GeoJsonFeatureCollection(
    String type,
    List<GeoJsonFeature> features
) {

  /**
   * Factory method creating a GeoJSON FeatureCollection from a list of features.
   *
   * @param features list of GeoJSON features
   * @return {@link GeoJsonFeatureCollection} instance
   */
  public static GeoJsonFeatureCollection of(List<GeoJsonFeature> features) {
    return new GeoJsonFeatureCollection("FeatureCollection", features);
  }

  /**
   * RFC 7946 compliant GeoJSON Feature.
   */
  public record GeoJsonFeature(
      String type,
      GeoJsonGeometry geometry,
      Map<String, Object> properties
  ) {

    /**
     * Factory method creating a Point feature.
     *
     * @param longitude longitude coordinate
     * @param latitude latitude coordinate
     * @param properties feature metadata properties map
     * @return {@link GeoJsonFeature}
     */
    public static GeoJsonFeature point(double longitude, double latitude, Map<String, Object> properties) {
      return new GeoJsonFeature("Feature", new GeoJsonGeometry("Point", List.of(longitude, latitude)), properties);
    }
  }

  /**
   * RFC 7946 compliant GeoJSON Geometry.
   */
  public record GeoJsonGeometry(
      String type,
      List<Double> coordinates
  ) {}
}
