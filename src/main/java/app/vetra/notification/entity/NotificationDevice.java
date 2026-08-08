package app.vetra.notification.entity;

import app.vetra.infrastructure.persistence.entity.BaseEntity;
import app.vetra.infrastructure.persistence.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Registered mobile or web client device token for push notification dispatch. */
@Entity
@Table(name = "notification_devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDevice extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "device_token", nullable = false, unique = true, length = 512)
  private String deviceToken;

  @Column(name = "platform", nullable = false, length = 32)
  @Builder.Default
  private String platform = "ANDROID";

  @Column(name = "app_version", length = 32)
  private String appVersion;

  @Column(name = "active", nullable = false)
  @Builder.Default
  private boolean active = true;

  @Column(name = "last_seen", nullable = false)
  @Builder.Default
  private Instant lastSeen = Instant.now();

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public String getDeviceToken() {
    return deviceToken;
  }

  public void setDeviceToken(String deviceToken) {
    this.deviceToken = deviceToken;
  }

  public String getPlatform() {
    return platform;
  }

  public void setPlatform(String platform) {
    this.platform = platform;
  }

  public String getAppVersion() {
    return appVersion;
  }

  public void setAppVersion(String appVersion) {
    this.appVersion = appVersion;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public Instant getLastSeen() {
    return lastSeen;
  }

  public void setLastSeen(Instant lastSeen) {
    this.lastSeen = lastSeen;
  }

  public static NotificationDeviceBuilder builder() {
    return new NotificationDeviceBuilder();
  }

  public static class NotificationDeviceBuilder {
    private User user;
    private String deviceToken;
    private String platform = "ANDROID";
    private String appVersion;
    private boolean active = true;
    private Instant lastSeen = Instant.now();

    public NotificationDeviceBuilder user(User user) {
      this.user = user;
      return this;
    }

    public NotificationDeviceBuilder deviceToken(String deviceToken) {
      this.deviceToken = deviceToken;
      return this;
    }

    public NotificationDeviceBuilder platform(String platform) {
      this.platform = platform;
      return this;
    }

    public NotificationDeviceBuilder appVersion(String appVersion) {
      this.appVersion = appVersion;
      return this;
    }

    public NotificationDeviceBuilder active(boolean active) {
      this.active = active;
      return this;
    }

    public NotificationDeviceBuilder lastSeen(Instant lastSeen) {
      this.lastSeen = lastSeen;
      return this;
    }

    public NotificationDevice build() {
      NotificationDevice device = new NotificationDevice();
      device.setUser(this.user);
      device.setDeviceToken(this.deviceToken);
      device.setPlatform(this.platform);
      device.setAppVersion(this.appVersion);
      device.setActive(this.active);
      device.setLastSeen(this.lastSeen);
      return device;
    }
  }
}
