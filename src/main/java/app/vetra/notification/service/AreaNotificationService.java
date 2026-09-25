package app.vetra.notification.service;

import app.vetra.auth.repository.FarmerProfileRepository;
import app.vetra.auth.repository.VetProfileRepository;
import app.vetra.infrastructure.persistence.entity.FarmerProfile;
import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.infrastructure.persistence.enums.VerificationStatus;
import app.vetra.notification.entity.NotificationChannel;
import app.vetra.notification.entity.NotificationPriority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Pushes a message to the farmers and vets whose registered location is inside a radius. */
@Service
public class AreaNotificationService {

  private static final Logger log = LoggerFactory.getLogger(AreaNotificationService.class);

  private final FarmerProfileRepository farmerProfileRepository;
  private final VetProfileRepository vetProfileRepository;
  private final NotificationService notificationService;

  public AreaNotificationService(
      FarmerProfileRepository farmerProfileRepository,
      VetProfileRepository vetProfileRepository,
      NotificationService notificationService) {
    this.farmerProfileRepository = farmerProfileRepository;
    this.vetProfileRepository = vetProfileRepository;
    this.notificationService = notificationService;
  }

  /** How many farmers and vets were sent the message. */
  public record Reach(int farmers, int vets) {}

  /**
   * Sends {@code farmerTitle/Body} to farmers and {@code vetTitle/Body} to vets within {@code
   * radiusKm} of the point. A null title skips that group.
   */
  // ponytail: scans all profiles in memory; move to a PostGIS ST_DWithin query past ~50k users.
  public Reach notifyWithin(
      double lat,
      double lng,
      double radiusKm,
      String farmerTitle,
      String farmerBody,
      String vetTitle,
      String vetBody,
      String payloadJson) {
    int farmers = 0;
    int vets = 0;
    if (farmerTitle != null) {
      for (FarmerProfile f : farmerProfileRepository.findAll()) {
        if (inside(f.getLatitude(), f.getLongitude(), lat, lng, radiusKm) && send(f.getUser().getId(),
            farmerTitle, farmerBody, payloadJson)) {
          farmers++;
        }
      }
    }
    if (vetTitle != null) {
      for (VetProfile v : vetProfileRepository.findAllActiveNonRejected(VerificationStatus.REJECTED)) {
        if (v.getUser() != null && inside(v.getLatitude(), v.getLongitude(), lat, lng, radiusKm)
            && send(v.getUser().getId(), vetTitle, vetBody, payloadJson)) {
          vets++;
        }
      }
    }
    log.info("[AREA-NOTIFY] {} farmers and {} vets within {} km of {},{}", farmers, vets, radiusKm, lat, lng);
    return new Reach(farmers, vets);
  }

  private boolean send(java.util.UUID userId, String title, String body, String payload) {
    try {
      notificationService.sendNotification(
          userId, title, body, payload, NotificationChannel.PUSH, NotificationPriority.HIGH);
      return true;
    } catch (Exception e) {
      log.warn("[AREA-NOTIFY] could not notify user {}: {}", userId, e.getMessage());
      return false;
    }
  }

  static boolean inside(Double lat, Double lng, double cLat, double cLng, double radiusKm) {
    if (lat == null || lng == null) {
      return false;
    }
    double dLat = Math.toRadians(lat - cLat);
    double dLng = Math.toRadians(lng - cLng);
    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
        + Math.cos(Math.toRadians(cLat)) * Math.cos(Math.toRadians(lat))
            * Math.sin(dLng / 2) * Math.sin(dLng / 2);
    return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)) <= radiusKm;
  }
}
