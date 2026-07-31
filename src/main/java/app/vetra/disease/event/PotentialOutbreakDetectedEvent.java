package app.vetra.disease.event;

/**
 * Event published when a geographic cluster of confirmed disease reports triggers potential outbreak detection.
 *
 * @param diseaseName disease name
 * @param centerLatitude centroid latitude
 * @param centerLongitude centroid longitude
 * @param reportCount number of affected reports in cluster
 */
public record PotentialOutbreakDetectedEvent(
    String diseaseName,
    Double centerLatitude,
    Double centerLongitude,
    int reportCount
) {}
