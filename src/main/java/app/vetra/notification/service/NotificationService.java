package app.vetra.notification.service;

import app.vetra.auth.repository.UserRepository;
import app.vetra.infrastructure.cache.CacheNames;
import app.vetra.infrastructure.exception.ResourceNotFoundException;
import app.vetra.infrastructure.exception.UnauthorizedResourceAccessException;
import app.vetra.infrastructure.metrics.VetraMetrics;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.notification.dto.NotificationResponse;
import app.vetra.notification.dto.UnreadCountResponse;
import app.vetra.notification.entity.Notification;
import app.vetra.notification.entity.NotificationChannel;
import app.vetra.notification.entity.NotificationDeliveryLog;
import app.vetra.notification.entity.NotificationDevice;
import app.vetra.notification.entity.NotificationPriority;
import app.vetra.notification.entity.NotificationStatus;
import app.vetra.notification.provider.NotificationProvider;
import app.vetra.notification.provider.NotificationProviderResult;
import app.vetra.notification.repository.NotificationDeliveryLogRepository;
import app.vetra.notification.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core notification dispatch service handling pipeline execution, provider invocation,
 * delivery logging, and inbox query management.
 */
@Service
public class NotificationService {

  private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

  private final NotificationRepository notificationRepository;
  private final NotificationDeliveryLogRepository logRepository;
  private final UserRepository userRepository;
  private final DeviceManagementService deviceService;
  private final List<NotificationProvider> notificationProviders;
  private final VetraMetrics vetraMetrics;

  /** Constructor injection. */
  public NotificationService(
      NotificationRepository notificationRepository,
      NotificationDeliveryLogRepository logRepository,
      UserRepository userRepository,
      DeviceManagementService deviceService,
      List<NotificationProvider> notificationProviders,
      VetraMetrics vetraMetrics) {
    this.notificationRepository = notificationRepository;
    this.logRepository = logRepository;
    this.userRepository = userRepository;
    this.deviceService = deviceService;
    this.notificationProviders = notificationProviders;
    this.vetraMetrics = vetraMetrics;
  }

  /**
   * Dispatches a notification to a specific target user.
   */
  @Transactional
  public NotificationResponse sendNotification(
      UUID userId, String title, String body, String payloadJson, NotificationChannel channel, NotificationPriority priority) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId, "USER_004"));

    Notification notification = Notification.builder()
        .user(user)
        .channel(channel != null ? channel : NotificationChannel.PUSH)
        .priority(priority != null ? priority : NotificationPriority.NORMAL)
        .title(title)
        .body(body)
        .payloadJson(payloadJson)
        .status(NotificationStatus.PENDING)
        .scheduledAt(Instant.now())
        .build();

    notification = notificationRepository.save(notification);

    // Attempt push delivery to registered devices
    List<NotificationDevice> activeDevices = deviceService.getUserActiveDevices(userId);
    NotificationProvider provider = resolveProvider(channel);

    if (activeDevices.isEmpty()) {
      log.info("No active registered push device tokens for user id={}. Message stored in inbox.", userId);
      notification.setStatus(NotificationStatus.QUEUED);
      notification = notificationRepository.save(notification);
      vetraMetrics.recordNotificationQueued();
    } else {
      boolean anyDelivered = false;
      for (NotificationDevice device : activeDevices) {
        NotificationProviderResult result = provider.send(notification, device.getDeviceToken());

        NotificationDeliveryLog deliveryLog = NotificationDeliveryLog.builder()
            .notification(notification)
            .provider(result.providerName())
            .status(result.success() ? "SUCCESS" : "FAILED")
            .responsePayload(result.messageId())
            .errorMessage(result.errorMessage())
            .attemptNumber(1)
            .build();

        logRepository.save(deliveryLog);

        if (result.success()) {
          anyDelivered = true;
        }
      }

      if (anyDelivered) {
        notification.setStatus(NotificationStatus.DELIVERED);
        notification.setDeliveredAt(Instant.now());
        vetraMetrics.recordNotificationSuccess();
      } else {
        notification.setStatus(NotificationStatus.FAILED);
        vetraMetrics.recordNotificationFailure();
      }
      notification = notificationRepository.save(notification);
    }

    return NotificationResponse.fromEntity(notification);
  }

  /** Lists user notifications with pagination. */
  @Transactional(readOnly = true)
  public Page<NotificationResponse> listUserNotifications(String userIdentifier, Pageable pageable) {
    User user = getUserByEmailOrPhone(userIdentifier);
    return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
        .map(NotificationResponse::fromEntity);
  }

  /** Gets unread notification count. */
  @Transactional(readOnly = true)
  @Cacheable(value = CacheNames.NOTIFICATIONS, key = "'unread_' + #userIdentifier")
  public UnreadCountResponse getUnreadCount(String userIdentifier) {
    User user = getUserByEmailOrPhone(userIdentifier);
    long count = notificationRepository.countByUserIdAndReadAtIsNull(user.getId());
    return new UnreadCountResponse(count);
  }

  /** Marks a notification as read. */
  @Transactional
  @CacheEvict(value = CacheNames.NOTIFICATIONS, key = "'unread_' + #userIdentifier")
  public NotificationResponse markAsRead(String userIdentifier, UUID notificationId) {
    User user = getUserByEmailOrPhone(userIdentifier);
    Notification notification = notificationRepository.findById(notificationId)
        .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId, "NOTIFICATION_002"));

    if (!notification.getUser().getId().equals(user.getId())) {
      throw new UnauthorizedResourceAccessException("Users can only access their own notifications", "NOTIFICATION_003");
    }

    if (notification.getReadAt() == null) {
      notification.setReadAt(Instant.now());
      notification.setStatus(NotificationStatus.READ);
      notification = notificationRepository.save(notification);
    }

    return NotificationResponse.fromEntity(notification);
  }

  private NotificationProvider resolveProvider(NotificationChannel channel) {
    return notificationProviders.stream()
        .filter(p -> p.providerName().equalsIgnoreCase("FCM"))
        .findFirst()
        .orElse(notificationProviders.get(0));
  }

  private User getUserByEmailOrPhone(String identifier) {
    return userRepository.findByEmail(identifier)
        .or(() -> userRepository.findByPhone(identifier))
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + identifier, "USER_004"));
  }
}
