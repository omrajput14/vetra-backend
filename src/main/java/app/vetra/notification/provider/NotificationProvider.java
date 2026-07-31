package app.vetra.notification.provider;

import app.vetra.notification.entity.Notification;

/**
 * Provider interface abstraction for notification delivery channels (FCM Push, Email, SMS, Webhook).
 */
public interface NotificationProvider {

  /**
   * Dispatches a notification payload to a target device token or endpoint.
   *
   * @param notification notification instance
   * @param targetDestination target device token, phone number, or email
   * @return {@link NotificationProviderResult}
   */
  NotificationProviderResult send(Notification notification, String targetDestination);

  /**
   * Performs provider health check.
   *
   * @return true if provider is operational
   */
  boolean health();

  /**
   * Identifier name of provider (e.g. "FCM", "SENDGRID", "TWILIO", "NOOP").
   *
   * @return provider name
   */
  String providerName();
}
