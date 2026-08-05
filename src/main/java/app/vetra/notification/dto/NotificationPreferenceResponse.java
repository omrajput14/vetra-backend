package app.vetra.notification.dto;

import app.vetra.notification.entity.NotificationPreference;
import java.util.UUID;

/** Public DTO representing a user's notification preferences. */
public record NotificationPreferenceResponse(
    UUID id,
    UUID userId,
    boolean appointmentNotifications,
    boolean vaccinationNotifications,
    boolean aiNotifications,
    boolean outbreakNotifications,
    boolean marketingNotifications) {

  /**
   * Factory method mapping NotificationPreference entity to NotificationPreferenceResponse DTO.
   *
   * @param pref entity instance
   * @return {@link NotificationPreferenceResponse} DTO
   */
  public static NotificationPreferenceResponse fromEntity(NotificationPreference pref) {
    return new NotificationPreferenceResponse(
        pref.getId(),
        pref.getUser().getId(),
        pref.isAppointmentNotifications(),
        pref.isVaccinationNotifications(),
        pref.isAiNotifications(),
        pref.isOutbreakNotifications(),
        pref.isMarketingNotifications());
  }
}
