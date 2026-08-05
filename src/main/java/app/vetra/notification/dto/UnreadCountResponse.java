package app.vetra.notification.dto;

/**
 * Payload returning unread notification count.
 *
 * @param unreadCount total unread notifications
 */
public record UnreadCountResponse(long unreadCount) {}
