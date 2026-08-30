package app.vetra.notification.provider;

import app.vetra.notification.config.FirebaseConfig;
import app.vetra.notification.entity.Notification;
import app.vetra.notification.entity.NotificationPriority;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Real Firebase Cloud Messaging (FCM) push notification provider using Firebase Admin SDK.
 * Dispatches targeted push notifications to mobile and web device tokens with custom payloads.
 */
@Component
public class FirebaseNotificationProvider implements NotificationProvider {

  private static final Logger log = LoggerFactory.getLogger(FirebaseNotificationProvider.class);
  private static final String DEFAULT_CHANNEL = "vetra_general";
  private static final String ALERT_CHANNEL = "vetra_clinical_alerts";

  private final FirebaseConfig firebaseConfig;
  private final ObjectMapper objectMapper;

  /** Constructor injection. */
  public FirebaseNotificationProvider(FirebaseConfig firebaseConfig, ObjectMapper objectMapper) {
    this.firebaseConfig = firebaseConfig;
    this.objectMapper = objectMapper;
  }

  @Override
  public NotificationProviderResult send(Notification notification, String deviceToken) {
    if (deviceToken == null || deviceToken.isBlank()) {
      return NotificationProviderResult.error(providerName(), "Invalid or empty device token");
    }

    if (!firebaseConfig.isInitialized()) {
      return sendDryRun(notification, deviceToken);
    }

    try {
      Message message = buildFcmMessage(notification, deviceToken);
      String fcmMessageId = FirebaseMessaging.getInstance().send(message);
      log.info(
          "[FCM SENT] messageId='{}' recipientUserId={} targetToken={}",
          fcmMessageId,
          notification.getUser().getId(),
          maskToken(deviceToken));

      return NotificationProviderResult.ok(providerName(), fcmMessageId);
    } catch (FirebaseMessagingException e) {
      log.error(
          "[FCM FAILURE] Error code='{}' message='{}' token={}",
          e.getMessagingErrorCode(),
          e.getMessage(),
          maskToken(deviceToken));
      return NotificationProviderResult.error(providerName(), e.getMessage());
    } catch (Exception e) {
      log.error("[FCM UNEXPECTED ERROR] Failed to send message: {}", e.getMessage(), e);
      return NotificationProviderResult.error(providerName(), e.getMessage());
    }
  }

  private Message buildFcmMessage(Notification notification, String deviceToken) {
    boolean isHighPriority =
        notification.getPriority() == NotificationPriority.HIGH
            || notification.getPriority() == NotificationPriority.CRITICAL;

    String channelId = isHighPriority ? ALERT_CHANNEL : DEFAULT_CHANNEL;

    AndroidConfig androidConfig =
        AndroidConfig.builder()
            .setPriority(
                isHighPriority
                    ? AndroidConfig.Priority.HIGH
                    : AndroidConfig.Priority.NORMAL)
            .setNotification(
                AndroidNotification.builder()
                    .setTitle(notification.getTitle())
                    .setBody(notification.getBody())
                    .setChannelId(channelId)
                    .setClickAction("FLUTTER_NOTIFICATION_CLICK")
                    .build())
            .build();

    Map<String, String> dataMap = extractDataMap(notification);

    return Message.builder()
        .setToken(deviceToken)
        .setNotification(
            com.google.firebase.messaging.Notification.builder()
                .setTitle(notification.getTitle())
                .setBody(notification.getBody())
                .build())
        .setAndroidConfig(androidConfig)
        .putAllData(dataMap)
        .build();
  }

  private Map<String, String> extractDataMap(Notification notification) {
    Map<String, String> data = new HashMap<>();
    data.put("notificationId", notification.getId() != null ? notification.getId().toString() : "");
    data.put("title", notification.getTitle() != null ? notification.getTitle() : "");
    data.put("body", notification.getBody() != null ? notification.getBody() : "");
    data.put("priority", notification.getPriority() != null ? notification.getPriority().name() : "NORMAL");

    if (notification.getPayloadJson() != null && !notification.getPayloadJson().isBlank()) {
      data.put("payloadJson", notification.getPayloadJson());
      try {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = objectMapper.readValue(notification.getPayloadJson(), Map.class);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
          if (entry.getValue() != null) {
            data.put(entry.getKey(), entry.getValue().toString());
          }
        }
      } catch (Exception e) {
        log.debug("Could not expand payloadJson into data map: {}", e.getMessage());
      }
    }
    return data;
  }

  private NotificationProviderResult sendDryRun(Notification notification, String deviceToken) {
    log.info(
        "[FCM DRY-RUN] title='{}' body='{}' targetToken={} priority={}",
        notification.getTitle(),
        notification.getBody(),
        maskToken(deviceToken),
        notification.getPriority());

    String fcmMessageId = "projects/vetra-1ebfa/messages/dry-run-" + UUID.randomUUID().toString();
    return NotificationProviderResult.ok(providerName(), fcmMessageId);
  }

  private String maskToken(String token) {
    if (token == null || token.length() <= 8) {
      return "***";
    }
    return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
  }

  @Override
  public boolean health() {
    return true;
  }

  @Override
  public String providerName() {
    return "FCM";
  }
}
