package app.vetra.notification.entity;

/** Communication delivery channels supported by the notification platform. */
public enum NotificationChannel {
  /** Push notification (e.g. Firebase Cloud Messaging / APNs). */
  PUSH,

  /** Email communication. */
  EMAIL,

  /** SMS text message. */
  SMS,

  /** Webhook callback dispatch. */
  WEBHOOK
}
