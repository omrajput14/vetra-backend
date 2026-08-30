package app.vetra.auth.service.impl;

import app.vetra.auth.dto.VetSummaryDto;
import app.vetra.auth.repository.VetProfileRepository;
import app.vetra.auth.service.VetDiscoveryService;
import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.infrastructure.persistence.enums.VerificationStatus;
import app.vetra.infrastructure.persistence.enums.VetMatchType;
import app.vetra.infrastructure.util.LocationNormalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of veterinarian discovery and ranking based on GPS proximity
 * and normalized administrative boundaries.
 */
@Service
public class VetDiscoveryServiceImpl implements VetDiscoveryService {

  private static final double DEFAULT_RADIUS_KM = 50.0;

  private final VetProfileRepository vetProfileRepository;

  public VetDiscoveryServiceImpl(VetProfileRepository vetProfileRepository) {
    this.vetProfileRepository = vetProfileRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<VetSummaryDto> searchNearbyVeterinarians(
      Double lat, Double lng, Double radiusKm, String village, String taluka, String district) {
    double effectiveRadius = (radiusKm != null && radiusKm > 0) ? radiusKm : DEFAULT_RADIUS_KM;
    List<VetProfile> allVets =
        vetProfileRepository.findAllActiveNonRejected(VerificationStatus.REJECTED);

    boolean hasLocationFilter = hasAnyFilter(lat, lng, village, taluka, district);
    List<VetSummaryDto> results = new ArrayList<>();

    for (VetProfile vet : allVets) {
      MatchEvaluation match =
          evaluateVet(vet, lat, lng, effectiveRadius, village, taluka, district, hasLocationFilter);
      if (match != null) {
        results.add(
            VetSummaryDto.fromEntity(vet, match.distanceKm(), match.matchType()));
      }
    }

    sortVeterinarians(results);
    return results;
  }

  private boolean hasAnyFilter(
      Double lat, Double lng, String village, String taluka, String district) {
    return (lat != null && lng != null)
        || isNonBlank(village)
        || isNonBlank(taluka)
        || isNonBlank(district);
  }

  private boolean isNonBlank(String str) {
    return str != null && !str.isBlank();
  }

  private MatchEvaluation evaluateVet(
      VetProfile vet,
      Double lat,
      Double lng,
      double radiusKm,
      String village,
      String taluka,
      String district,
      boolean hasFilter) {
    Double gpsDistance =
        LocationNormalizer.calculateHaversineDistanceKm(
            lat, lng, vet.getLatitude(), vet.getLongitude());

    if (gpsDistance != null && gpsDistance <= radiusKm) {
      return new MatchEvaluation(VetMatchType.GPS_RADIUS, gpsDistance);
    }
    if (isNonBlank(village) && LocationNormalizer.matches(village, vet.getVillage())) {
      return new MatchEvaluation(VetMatchType.SAME_VILLAGE, gpsDistance);
    }
    if (isNonBlank(taluka) && LocationNormalizer.matches(taluka, vet.getTaluka())) {
      return new MatchEvaluation(VetMatchType.SAME_TALUKA, gpsDistance);
    }
    if (isNonBlank(district) && LocationNormalizer.matches(district, vet.getDistrict())) {
      return new MatchEvaluation(VetMatchType.SAME_DISTRICT, gpsDistance);
    }
    if (!hasFilter) {
      return new MatchEvaluation(VetMatchType.GENERAL, gpsDistance);
    }
    return null;
  }

  private void sortVeterinarians(List<VetSummaryDto> vets) {
    vets.sort(
        Comparator.comparingInt((VetSummaryDto v) -> getPriority(v.matchType()))
            .thenComparing(v -> v.distanceKm() != null ? v.distanceKm() : Double.MAX_VALUE)
            .thenComparing((VetSummaryDto v) -> v.rating() != null ? -v.rating() : 0.0)
            .thenComparing(VetSummaryDto::name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
  }

  private int getPriority(VetMatchType matchType) {
    if (matchType == null) {
      return 5;
    }
    return switch (matchType) {
      case GPS_RADIUS -> 1;
      case SAME_VILLAGE -> 2;
      case SAME_TALUKA -> 3;
      case SAME_DISTRICT -> 4;
      case GENERAL -> 5;
    };
  }

  private record MatchEvaluation(VetMatchType matchType, Double distanceKm) {}
}
