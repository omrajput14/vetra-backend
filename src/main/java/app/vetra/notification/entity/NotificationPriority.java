package app.vetra.notification.entity;

/**
 * Priority levels for notification delivery scheduling and override rules.
 */
public enum NotificationPriority {
  /** Low priority background info. */
  LOW,

  /** Standard priority notification. */
  NORMAL,

  /** High priority event notification. */
  HIGH,

  /** Critical priority (emergency outbreak alerts overriding user opt-outs). */
  CRITICAL
}
