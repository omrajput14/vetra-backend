package app.vetra.disease.geo;

/**
 * Geo-spatial utilities for coordinate validation and Haversine distance calculations.
 */
public final class GeoUtils {

  private static final double EARTH_RADIUS_KM = 6371.0088;

  private GeoUtils() {}

  /**
   * Validates latitude (-90 to +90) and longitude (-180 to +180) bounds.
   *
   * @param latitude latitude coordinate
   * @param longitude longitude coordinate
   * @return true if coordinates are within valid geographic ranges
   */
  public static boolean isValidCoordinate(Double latitude, Double longitude) {
    if (latitude == null || longitude == null) {
      return false;
    }
    return latitude >= -90.0 && latitude <= 90.0 && longitude >= -180.0 && longitude <= 180.0;
  }

  /**
   * Calculates Haversine distance in kilometers between two geographic points.
   *
   * @param lat1 point 1 latitude
   * @param lon1 point 1 longitude
   * @param lat2 point 2 latitude
   * @param lon2 point 2 longitude
   * @return distance in kilometers
   */
  public static double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);

    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
        + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
        * Math.sin(dLon / 2) * Math.sin(dLon / 2);

    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return EARTH_RADIUS_KM * c;
  }
}
