package app.vetra.infrastructure.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit test suite for LocationNormalizer utility. */
class LocationNormalizerTest {

  @Test
  @DisplayName("normalize trims and collapses multiple whitespaces into single space and converts to lowercase")
  void testNormalizeLocation() {
    assertEquals("khed", LocationNormalizer.normalize("  Khed  "));
    assertEquals("baramati west", LocationNormalizer.normalize("  BARAMATI   WEST  "));
    assertEquals("pune", LocationNormalizer.normalize("Pune"));
    assertEquals("", LocationNormalizer.normalize(null));
    assertEquals("", LocationNormalizer.normalize("   "));
  }

  @Test
  @DisplayName("matches returns true for location variations with whitespace and case differences")
  void testMatches() {
    assertTrue(LocationNormalizer.matches("Baramati", "baramati"));
    assertTrue(LocationNormalizer.matches(" Khed ", "khed"));
    assertTrue(LocationNormalizer.matches("Haveli   Taluka", "haveli taluka"));
    assertFalse(LocationNormalizer.matches("Baramati", "Haveli"));
    assertFalse(LocationNormalizer.matches(null, "Pune"));
    assertFalse(LocationNormalizer.matches("Pune", null));
    assertFalse(LocationNormalizer.matches("  ", "Pune"));
  }

  @Test
  @DisplayName("calculateHaversineDistanceKm calculates accurate great-circle distance")
  void testCalculateHaversineDistance() {
    // Mumbai (19.0760, 72.8777) to Pune (18.5204, 73.8567) is approx 119-122 km
    Double distance =
        LocationNormalizer.calculateHaversineDistanceKm(19.0760, 72.8777, 18.5204, 73.8567);
    assertNotNull(distance);
    assertTrue(distance >= 115.0 && distance <= 125.0);

    // Identical coordinates should return 0.0
    Double samePoint =
        LocationNormalizer.calculateHaversineDistanceKm(18.5204, 73.8567, 18.5204, 73.8567);
    assertNotNull(samePoint);
    assertEquals(0.0, samePoint);

    // Null or invalid coordinates should return null
    assertNull(LocationNormalizer.calculateHaversineDistanceKm(null, 73.8567, 18.5204, 73.8567));
    assertNull(LocationNormalizer.calculateHaversineDistanceKm(100.0, 73.8567, 18.5204, 73.8567));
    assertNull(LocationNormalizer.calculateHaversineDistanceKm(18.5204, 200.0, 18.5204, 73.8567));
  }
}
