package app.vetra.auth.service;

import app.vetra.auth.dto.VetSummaryDto;
import java.util.List;

/**
 * Service for discovering and prioritizing nearby veterinarians based on GPS distance
 * and normalized administrative boundaries (village, taluka, district).
 */
public interface VetDiscoveryService {

  /**
   * Discovers and ranks nearby veterinarians using hierarchical matching:
   * 1. GPS_RADIUS (nearest distance first)
   * 2. SAME_VILLAGE (normalized match)
   * 3. SAME_TALUKA (normalized match)
   * 4. SAME_DISTRICT (normalized match)
   * 5. GENERAL (directory fallback when no filter supplied)
   *
   * @param lat farmer latitude (optional)
   * @param lng farmer longitude (optional)
   * @param radiusKm search radius in kilometers (optional, defaults to 50.0)
   * @param village farmer village (optional)
   * @param taluka farmer taluka / sub-district (optional)
   * @param district farmer district (optional)
   * @return prioritized list of veterinarian summary DTOs
   */
  List<VetSummaryDto> searchNearbyVeterinarians(
      Double lat, Double lng, Double radiusKm, String village, String taluka, String district);
}
