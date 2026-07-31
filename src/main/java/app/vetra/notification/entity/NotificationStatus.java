package app.vetra.notification.entity;

/**
 * Delivery status lifecycle of a notification message.
 */
public enum NotificationStatus {
  /** Created and awaiting processing. */
  PENDING,

  /** Enqueued for provider dispatch. */
  QUEUED,

  /** Successfully accepted by provider gateway. */
  SENT,

  /** Confirmed delivered to end-user device. */
  DELIVERED,

  /** Marked read by end-user. */
  READ,

  /** Failed to deliver. */
  FAILED
}
