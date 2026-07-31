package app.vetra.notification.dto;

/**
 * Payload for updating user notification preferences.
 */
public record UpdatePreferenceRequest(
    Boolean appointmentNotifications,
    Boolean vaccinationNotifications,
    Boolean aiNotifications,
    Boolean outbreakNotifications,
    Boolean marketingNotifications
) {}
