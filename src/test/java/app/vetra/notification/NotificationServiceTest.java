package app.vetra.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.auth.dto.FarmerRegisterRequest;
import app.vetra.auth.service.AuthService;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.notification.dto.NotificationPreferenceResponse;
import app.vetra.notification.dto.NotificationResponse;
import app.vetra.notification.dto.RegisterDeviceRequest;
import app.vetra.notification.dto.UnreadCountResponse;
import app.vetra.notification.dto.UpdatePreferenceRequest;
import app.vetra.notification.entity.NotificationChannel;
import app.vetra.notification.entity.NotificationDevice;
import app.vetra.notification.entity.NotificationPriority;
import app.vetra.notification.service.DeviceManagementService;
import app.vetra.notification.service.NotificationPreferenceService;
import app.vetra.notification.service.NotificationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Unit & Integration test suite for Notification & Communication Platform (Stage 11), device token
 * registration, FCM push dispatch, preference validation, and unread counts.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:vetra_notify_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
      "spring.flyway.enabled=false",
      "vetra.jwt.secret=test-jwt-secret-value-minimum-32-characters-long",
      "vetra.jwt.expiration-ms=86400000",
      "vetra.jwt.refresh-expiration-ms=604800000",
      "vetra.cors.allowed-origins=http://localhost:3000",
      "vetra.cors.allowed-methods=GET,POST,PUT,DELETE,PATCH,OPTIONS",
      "vetra.cors.allowed-headers=*",
      "vetra.cors.allow-credentials=true",
      "vetra.cors.max-age=3600",
      "vetra.aws.region=ap-south-1",
      "vetra.aws.credentials.access-key=test-key",
      "vetra.aws.credentials.secret-key=test-secret",
      "vetra.aws.s3.bucket-name=vetra-test-bucket",
      "vetra.aws.s3.presigned-url-expiry-minutes=15",
      "vetra.ai.enabled=false",
      "vetra.ai.default-provider=NONE",
      "vetra.ai.retry.max-attempts=1",
      "vetra.ai.retry.backoff=1ms"
    })
class NotificationServiceTest {

  @Autowired private NotificationService notificationService;
  @Autowired private DeviceManagementService deviceService;
  @Autowired private NotificationPreferenceService preferenceService;
  @Autowired private AuthService authService;

  @Test
  void testDeviceRegistrationAndRetrieval() {
    authService.registerFarmer(
        new FarmerRegisterRequest(
            "farmer_dev@vetra.app",
            "+1555666001",
            "pass123",
            "Farmer Dev",
            "Farm",
            "Village",
            "District",
            "State",
            12.0,
            77.0,
            10));

    NotificationDevice device =
        deviceService.registerDevice(
            "farmer_dev@vetra.app",
            new RegisterDeviceRequest("FCM-TOKEN-XYZ-123", "ANDROID", "1.0.0"));

    assertNotNull(device.getId());
    assertEquals("FCM-TOKEN-XYZ-123", device.getDeviceToken());
    assertTrue(device.isActive());

    List<NotificationDevice> activeDevices =
        deviceService.getUserActiveDevices(device.getUser().getId());
    assertFalse(activeDevices.isEmpty());
  }

  @Test
  void testNotificationPreferencesManagement() {
    authService.registerFarmer(
        new FarmerRegisterRequest(
            "farmer_pref@vetra.app",
            "+1555666002",
            "pass123",
            "Farmer Pref",
            "Farm",
            "Village",
            "District",
            "State",
            12.0,
            77.0,
            10));

    NotificationPreferenceResponse prefs =
        preferenceService.getPreferences("farmer_pref@vetra.app");
    assertTrue(prefs.appointmentNotifications());
    assertTrue(prefs.outbreakNotifications());

    NotificationPreferenceResponse updated =
        preferenceService.updatePreferences(
            "farmer_pref@vetra.app", new UpdatePreferenceRequest(false, true, true, true, true));

    assertFalse(updated.appointmentNotifications());
    assertTrue(updated.marketingNotifications());
  }

  @Test
  void testNotificationSendAndMarkAsRead() {
    authService.registerFarmer(
        new FarmerRegisterRequest(
            "farmer_send@vetra.app",
            "+1555666003",
            "pass123",
            "Farmer Send",
            "Farm",
            "Village",
            "District",
            "State",
            12.0,
            77.0,
            10));

    NotificationDevice device =
        deviceService.registerDevice(
            "farmer_send@vetra.app",
            new RegisterDeviceRequest("FCM-TOKEN-SEND-999", "ANDROID", "1.0.0"));

    User user = device.getUser();

    NotificationResponse sent =
        notificationService.sendNotification(
            user.getId(),
            "Test Title",
            "Test Body",
            "{}",
            NotificationChannel.PUSH,
            NotificationPriority.NORMAL);

    assertNotNull(sent.id());
    assertEquals("Test Title", sent.title());

    UnreadCountResponse unread = notificationService.getUnreadCount("farmer_send@vetra.app");
    assertEquals(1, unread.unreadCount());

    NotificationResponse read = notificationService.markAsRead("farmer_send@vetra.app", sent.id());
    assertNotNull(read.readAt());

    UnreadCountResponse unreadAfter = notificationService.getUnreadCount("farmer_send@vetra.app");
    assertEquals(0, unreadAfter.unreadCount());
  }
}
