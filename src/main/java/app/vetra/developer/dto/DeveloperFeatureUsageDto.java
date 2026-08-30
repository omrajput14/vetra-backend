package app.vetra.developer.dto;

import java.util.List;

/** Feature adoption matrix payload. */
public record DeveloperFeatureUsageDto(
    List<FeatureAdoptionItemDto> features,
    long totalTrackedFeatures,
    long totalUntrackedFeatures) {

  /** Individual feature adoption and tracking status item. */
  public record FeatureAdoptionItemDto(
      String featureKey,
      String featureName,
      String category,
      TelemetryClassification classification,
      Long usageCount,
      Long uniqueUsersCount,
      String description,
      String dataSource,
      String lastEventAt) {}
}
