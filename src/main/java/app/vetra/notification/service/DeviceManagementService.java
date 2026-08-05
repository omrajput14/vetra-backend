package app.vetra.notification.service;

import app.vetra.auth.repository.UserRepository;
import app.vetra.infrastructure.exception.ResourceNotFoundException;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.notification.dto.RegisterDeviceRequest;
import app.vetra.notification.entity.NotificationDevice;
import app.vetra.notification.repository.NotificationDeviceRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service managing mobile/web device registration and token lifecycle. */
@Service
public class DeviceManagementService {

  private static final Logger log = LoggerFactory.getLogger(DeviceManagementService.class);

  private final NotificationDeviceRepository deviceRepository;
  private final UserRepository userRepository;

  /** Constructor injection. */
  public DeviceManagementService(
      NotificationDeviceRepository deviceRepository, UserRepository userRepository) {
    this.deviceRepository = deviceRepository;
    this.userRepository = userRepository;
  }

  /** Registers or refreshes a device token for push notification delivery. */
  @Transactional
  public NotificationDevice registerDevice(String userIdentifier, RegisterDeviceRequest request) {
    User user = getUserByEmailOrPhone(userIdentifier);
    Optional<NotificationDevice> existing =
        deviceRepository.findByDeviceToken(request.deviceToken());

    if (existing.isPresent()) {
      NotificationDevice device = existing.get();
      device.setUser(user);
      device.setActive(true);
      device.setPlatform(request.platform() != null ? request.platform() : "ANDROID");
      device.setAppVersion(request.appVersion());
      device.setLastSeen(Instant.now());
      log.info("Refreshed active push device token id={} userId={}", device.getId(), user.getId());
      return deviceRepository.save(device);
    }

    NotificationDevice device =
        NotificationDevice.builder()
            .user(user)
            .deviceToken(request.deviceToken())
            .platform(request.platform() != null ? request.platform() : "ANDROID")
            .appVersion(request.appVersion())
            .active(true)
            .lastSeen(Instant.now())
            .build();

    device = deviceRepository.save(device);
    log.info("Registered new push device token id={} userId={}", device.getId(), user.getId());
    return device;
  }

  /** Deactivates a device token. */
  @Transactional
  public void deactivateDevice(String userIdentifier, UUID deviceId) {
    User user = getUserByEmailOrPhone(userIdentifier);
    NotificationDevice device =
        deviceRepository
            .findById(deviceId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Device token not found: " + deviceId, "NOTIFICATION_001"));

    if (device.getUser().getId().equals(user.getId())) {
      device.setActive(false);
      deviceRepository.save(device);
    }
  }

  /** Lists active devices for a user. */
  @Transactional(readOnly = true)
  public List<NotificationDevice> getUserActiveDevices(UUID userId) {
    return deviceRepository.findByUserIdAndActiveTrue(userId);
  }

  private User getUserByEmailOrPhone(String identifier) {
    return userRepository
        .findByEmail(identifier)
        .or(() -> userRepository.findByPhone(identifier))
        .orElseThrow(
            () -> new ResourceNotFoundException("User not found: " + identifier, "USER_004"));
  }
}
