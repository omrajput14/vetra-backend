package app.vetra.appointment.service;

import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.entity.Appointment;
import app.vetra.infrastructure.persistence.entity.FarmerProfile;
import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.infrastructure.persistence.enums.AppointmentStatus;
import app.vetra.notification.entity.NotificationChannel;
import app.vetra.notification.entity.NotificationPriority;
import app.vetra.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Helper service managing push notifications dispatched during appointment lifecycle events. */
@Component
public class AppointmentNotificationHelper {

  private static final Logger log = LoggerFactory.getLogger(AppointmentNotificationHelper.class);

  private final NotificationService notificationService;

  public AppointmentNotificationHelper(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  /** Sends booking request notifications to both farmer and veterinarian. */
  public void notifyBookingRequested(
      Appointment saved, FarmerProfile farmer, VetProfile vet, Animal animal) {
    if (notificationService == null) {
      return;
    }
    try {
      String animalLabel =
          animal.getAnimalName() != null ? animal.getAnimalName() : animal.getTagNumber();
      String farmerPayload =
          "{\"appointmentId\":\""
              + saved.getId()
              + "\",\"route\":\"/appointment-details?id="
              + saved.getId()
              + "\"}";

      notificationService.sendNotification(
          farmer.getUser().getId(),
          "Appointment Requested",
          "Appointment requested with Dr. "
              + vet.getFullName()
              + " on "
              + saved.getAppointmentDate()
              + " for "
              + animalLabel
              + ".",
          farmerPayload,
          NotificationChannel.PUSH,
          NotificationPriority.NORMAL);

      notificationService.sendNotification(
          vet.getUser().getId(),
          "New Consultation Request",
          farmer.getFullName()
              + " requested a consultation for "
              + animalLabel
              + " on "
              + saved.getAppointmentDate()
              + ".",
          farmerPayload,
          NotificationChannel.PUSH,
          NotificationPriority.NORMAL);
    } catch (Exception e) {
      log.warn("Failed to dispatch appointment booking notifications: {}", e.getMessage());
    }
  }

  /** Sends status transition push notifications to the farmer. */
  public void dispatchStatusNotification(Appointment appointment, AppointmentStatus status) {
    if (notificationService == null || appointment.getFarmer() == null) {
      return;
    }
    try {
      String vetName =
          appointment.getVeterinarian() != null
              ? appointment.getVeterinarian().getFullName()
              : "Veterinarian";
      String title = getNotificationTitle(status);
      String body = getNotificationBody(status, vetName, appointment.getAppointmentDate().toString());

      if (title != null && appointment.getFarmer().getUser() != null) {
        String payload =
            "{\"appointmentId\":\""
                + appointment.getId()
                + "\",\"route\":\"/appointment-details?id="
                + appointment.getId()
                + "\"}";
        notificationService.sendNotification(
            appointment.getFarmer().getUser().getId(),
            title,
            body,
            payload,
            NotificationChannel.PUSH,
            NotificationPriority.HIGH);
      }
    } catch (Exception e) {
      log.warn("Failed to dispatch appointment status notification: {}", e.getMessage());
    }
  }

  private String getNotificationTitle(AppointmentStatus status) {
    return switch (status) {
      case CONFIRMED -> "Appointment Confirmed";
      case EN_ROUTE -> "Veterinarian En Route";
      case ARRIVED -> "Veterinarian Arrived";
      case CANCELLED -> "Appointment Cancelled";
      case REJECTED -> "Appointment Declined";
      default -> null;
    };
  }

  private String getNotificationBody(AppointmentStatus status, String vetName, String dateStr) {
    return switch (status) {
      case CONFIRMED -> "Dr. " + vetName + " confirmed your appointment on " + dateStr + ".";
      case EN_ROUTE -> "Dr. " + vetName + " is on the way to your farm for appointment on " + dateStr + ".";
      case ARRIVED -> "Dr. " + vetName + " has arrived for your appointment.";
      case CANCELLED -> "Appointment on " + dateStr + " with Dr. " + vetName + " was cancelled.";
      case REJECTED -> "Dr. " + vetName + " was unable to accept your appointment on " + dateStr + ".";
      default -> "";
    };
  }
}
