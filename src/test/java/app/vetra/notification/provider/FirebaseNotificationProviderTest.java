package app.vetra.notification.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.notification.config.FirebaseConfig;
import app.vetra.notification.entity.Notification;
import app.vetra.notification.entity.NotificationPriority;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FirebaseNotificationProviderTest {

  private FirebaseNotificationProvider provider;
  private FirebaseConfig firebaseConfig;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    firebaseConfig = new FirebaseConfig();
    objectMapper = new ObjectMapper();
    provider = new FirebaseNotificationProvider(firebaseConfig, objectMapper);
  }

  @Test
  void testProviderNameAndHealth() {
    assertEquals("FCM", provider.providerName());
    assertTrue(provider.health());
  }

  @Test
  void testSendWithEmptyOrNullTokenReturnsError() {
    Notification notification =
        Notification.builder()
            .title("Appointment Alert")
            .body("Your appointment is confirmed")
            .build();

    NotificationProviderResult nullResult = provider.send(notification, null);
    assertFalse(nullResult.success());
    assertEquals("FCM", nullResult.providerName());

    NotificationProviderResult emptyResult = provider.send(notification, "   ");
    assertFalse(emptyResult.success());
  }

  @Test
  void testSendWithDryRunFallbackDispatchesValidMessageId() {
    User dummyUser = new User();
    Notification notification =
        Notification.builder()
            .user(dummyUser)
            .title("New Consultation Request")
            .body("Farmer Ramesh requested a consultation for Gauri Cow.")
            .payloadJson("{\"appointmentId\":\"appt-100\",\"route\":\"/appointment-details?id=appt-100\"}")
            .priority(NotificationPriority.HIGH)
            .build();

    NotificationProviderResult result = provider.send(notification, "fcm-device-token-123456789");
    assertTrue(result.success());
    assertNotNull(result.messageId());
    assertTrue(result.messageId().startsWith("projects/vetra-1ebfa/messages/"));
  }
}
