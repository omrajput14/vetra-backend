package app.vetra.disease.geo;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

/** RFC 7946 compliant GeoJSON FeatureCollection wrapper. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeoJsonFeatureCollection(String type, List<GeoJsonFeature> features) {

  /**
   * Factory method creating a GeoJSON FeatureCollection from a list of features.
   *
   * @param features list of GeoJSON features
   * @return {@link GeoJsonFeatureCollection} instance
   */
  public static GeoJsonFeatureCollection of(List<GeoJsonFeature> features) {
    return new GeoJsonFeatureCollection("FeatureCollection", features);
  }

  /** RFC 7946 compliant GeoJSON Feature. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record GeoJsonFeature(
      String type, GeoJsonGeometry geometry, Map<String, Object> properties) {

    /**
     * Factory method creating a Point feature.
     *
     * @param longitude longitude coordinate
     * @param latitude latitude coordinate
     * @param properties feature metadata properties map
     * @return {@link GeoJsonFeature}
     */
    public static GeoJsonFeature point(
        double longitude, double latitude, Map<String, Object> properties) {
      return new GeoJsonFeature(
          "Feature", new GeoJsonGeometry("Point", List.of(longitude, latitude)), properties);
    }

    /**
     * Factory method creating a generic GeoJSON feature (Polygon, MultiPolygon, Point).
     *
     * @param geometryType geometry type
     * @param coordinates geometry coordinates (List, nested Lists, or GeoJSON coordinates array)
     * @param properties metadata properties
     * @return {@link GeoJsonFeature}
     */
    public static GeoJsonFeature of(
        String geometryType, Object coordinates, Map<String, Object> properties) {
      return new GeoJsonFeature(
          "Feature", new GeoJsonGeometry(geometryType, coordinates), properties);
    }
  }

  /** RFC 7946 compliant GeoJSON Geometry supporting Points, Polygons, and MultiPolygons. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record GeoJsonGeometry(String type, Object coordinates) {}
}
