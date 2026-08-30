package app.vetra.infrastructure.util;

import java.util.Locale;

/**
 * Reusable utility for normalizing and comparing administrative location names
 * (village, taluka, district, state) and calculating Haversine geographical distances.
 */
public final class LocationNormalizer {

  private static final double EARTH_RADIUS_KM = 6371.0;

  private LocationNormalizer() {
    // Utility class private constructor
  }

  /**
   * Normalizes an administrative location string by trimming, collapsing repeated whitespace,
   * and converting to lower case.
   *
   * @param location raw location string
   * @return normalized location string or empty string if input is null/blank
   */
  public static String normalize(String location) {
    if (location == null || location.isBlank()) {
      return "";
    }
    return location.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
  }

  /**
   * Checks if two administrative location strings match under normalization.
   * Returns false if either string is null or blank.
   *
   * @param locationA first location string
   * @param locationB second location string
   * @return true if both strings represent the same normalized location
   */
  public static boolean matches(String locationA, String locationB) {
    String normA = normalize(locationA);
    String normB = normalize(locationB);
    if (normA.isEmpty() || normB.isEmpty()) {
      return false;
    }
    return normA.equals(normB);
  }

  /**
   * Calculates Haversine great-circle distance between two GPS coordinates in kilometers.
   *
   * @param lat1 latitude of point 1
   * @param lon1 longitude of point 1
   * @param lat2 latitude of point 2
   * @param lon2 longitude of point 2
   * @return distance in kilometers, or null if any coordinate is null or invalid
   */
  public static Double calculateHaversineDistanceKm(
      Double lat1, Double lon1, Double lat2, Double lon2) {
    if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
      return null;
    }
    if (lat1 < -90.0 || lat1 > 90.0 || lat2 < -90.0 || lat2 > 90.0) {
      return null;
    }
    if (lon1 < -180.0 || lon1 > 180.0 || lon2 < -180.0 || lon2 > 180.0) {
      return null;
    }

    double latDistance = Math.toRadians(lat2 - lat1);
    double lonDistance = Math.toRadians(lon2 - lon1);

    double a =
        Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
            + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2)
                * Math.sin(lonDistance / 2);

    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    double distance = EARTH_RADIUS_KM * c;

    // Round to 1 decimal place for clean user display
    return Math.round(distance * 10.0) / 10.0;
  }
}
