package app.vetra.disease.dto;

import app.vetra.disease.entity.DiseaseReport;

/**
 * DTO representing a nearby disease report payload with calculated distance in kilometers.
 *
 * @param report disease report DTO
 * @param distanceKm distance in kilometers from search center
 */
public record NearbyReportResponse(
    DiseaseReportResponse report,
    double distanceKm
) {

  /**
   * Factory method mapping DiseaseReport entity and calculated distance to NearbyReportResponse DTO.
   *
   * @param report entity instance
   * @param distanceKm calculated distance in kilometers
   * @return {@link NearbyReportResponse} DTO
   */
  public static NearbyReportResponse from(DiseaseReport report, double distanceKm) {
    return new NearbyReportResponse(DiseaseReportResponse.fromEntity(report), distanceKm);
  }
}
