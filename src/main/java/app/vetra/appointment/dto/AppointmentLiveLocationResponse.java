package app.vetra.appointment.dto;

import java.time.Instant;

/**
 * Public response DTO for farmer-side live tracking of veterinarian en-route progress.
 * Real GPS coordinates and Haversine distance only; no fabricated driving ETAs.
 */
public record AppointmentLiveLocationResponse(
    boolean isLive,
    Double latitude,
    Double longitude,
    Double distanceKm,
    String status,
    String message,
    Instant updatedAt) {

  /** Factory for non-live responses when appointment is not active or en-route. */
  public static AppointmentLiveLocationResponse notLive(String status, String message) {
    return new AppointmentLiveLocationResponse(false, null, null, null, status, message, null);
  }

  /** Factory for live responses with valid coordinates and calculated distance. */
  public static AppointmentLiveLocationResponse live(
      Double latitude, Double longitude, Double distanceKm, String status, Instant updatedAt) {
    return new AppointmentLiveLocationResponse(
        true, latitude, longitude, distanceKm, status, "Veterinarian is en route to your farm", updatedAt);
  }
}
