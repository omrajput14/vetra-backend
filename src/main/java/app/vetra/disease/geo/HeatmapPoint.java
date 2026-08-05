package app.vetra.disease.geo;

/**
 * Spatial heatmap hotspot point payload with normalized intensity weight.
 *
 * @param latitude centroid latitude
 * @param longitude centroid longitude
 * @param intensityWeight normalized weight (0.0 to 1.0)
 * @param caseCount number of reported cases
 * @param diseaseName target disease name
 */
public record HeatmapPoint(
    Double latitude, Double longitude, double intensityWeight, int caseCount, String diseaseName) {}
