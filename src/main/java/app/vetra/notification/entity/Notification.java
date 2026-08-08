package app.vetra.notification.entity;

import app.vetra.infrastructure.persistence.entity.BaseEntity;
import app.vetra.infrastructure.persistence.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/** Entity representing an individual notification instance dispatched to a user. */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "template_id")
  private NotificationTemplate template;

  @Enumerated(EnumType.STRING)
  @Column(name = "channel", nullable = false, length = 32)
  @Builder.Default
  private NotificationChannel channel = NotificationChannel.PUSH;

  @Enumerated(EnumType.STRING)
  @Column(name = "priority", nullable = false, length = 32)
  @Builder.Default
  private NotificationPriority priority = NotificationPriority.NORMAL;

  @Column(name = "title", nullable = false, length = 256)
  private String title;

  @Column(name = "body", nullable = false, columnDefinition = "TEXT")
  private String body;

  @Column(name = "payload_json", columnDefinition = "TEXT")
  private String payloadJson;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  @Builder.Default
  private NotificationStatus status = NotificationStatus.PENDING;

  @Column(name = "scheduled_at")
  @Builder.Default
  private Instant scheduledAt = Instant.now();

  @Column(name = "delivered_at")
  private Instant deliveredAt;

  @Column(name = "read_at")
  private Instant readAt;

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public NotificationTemplate getTemplate() {
    return template;
  }

  public void setTemplate(NotificationTemplate template) {
    this.template = template;
  }

  public NotificationChannel getChannel() {
    return channel;
  }

  public void setChannel(NotificationChannel channel) {
    this.channel = channel;
  }

  public NotificationPriority getPriority() {
    return priority;
  }

  public void setPriority(NotificationPriority priority) {
    this.priority = priority;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public String getPayloadJson() {
    return payloadJson;
  }

  public void setPayloadJson(String payloadJson) {
    this.payloadJson = payloadJson;
  }

  public NotificationStatus getStatus() {
    return status;
  }

  public void setStatus(NotificationStatus status) {
    this.status = status;
  }

  public Instant getScheduledAt() {
    return scheduledAt;
  }

  public void setScheduledAt(Instant scheduledAt) {
    this.scheduledAt = scheduledAt;
  }

  public Instant getDeliveredAt() {
    return deliveredAt;
  }

  public void setDeliveredAt(Instant deliveredAt) {
    this.deliveredAt = deliveredAt;
  }

  public Instant getReadAt() {
    return readAt;
  }

  public void setReadAt(Instant readAt) {
    this.readAt = readAt;
  }

  public static NotificationBuilder builder() {
    return new NotificationBuilder();
  }

  public static class NotificationBuilder {
    private User user;
    private NotificationTemplate template;
    private NotificationChannel channel = NotificationChannel.PUSH;
    private NotificationPriority priority = NotificationPriority.NORMAL;
    private String title;
    private String body;
    private String payloadJson;
    private NotificationStatus status = NotificationStatus.PENDING;
    private Instant scheduledAt = Instant.now();
    private Instant deliveredAt;
    private Instant readAt;

    public NotificationBuilder user(User user) {
      this.user = user;
      return this;
    }

    public NotificationBuilder template(NotificationTemplate template) {
      this.template = template;
      return this;
    }

    public NotificationBuilder channel(NotificationChannel channel) {
      this.channel = channel;
      return this;
    }

    public NotificationBuilder priority(NotificationPriority priority) {
      this.priority = priority;
      return this;
    }

    public NotificationBuilder title(String title) {
      this.title = title;
      return this;
    }

    public NotificationBuilder body(String body) {
      this.body = body;
      return this;
    }

    public NotificationBuilder payloadJson(String payloadJson) {
      this.payloadJson = payloadJson;
      return this;
    }

    public NotificationBuilder status(NotificationStatus status) {
      this.status = status;
      return this;
    }

    public NotificationBuilder scheduledAt(Instant scheduledAt) {
      this.scheduledAt = scheduledAt;
      return this;
    }

    public NotificationBuilder deliveredAt(Instant deliveredAt) {
      this.deliveredAt = deliveredAt;
      return this;
    }

    public NotificationBuilder readAt(Instant readAt) {
      this.readAt = readAt;
      return this;
    }

    public Notification build() {
      Notification notification = new Notification();
      notification.setUser(this.user);
      notification.setTemplate(this.template);
      notification.setChannel(this.channel);
      notification.setPriority(this.priority);
      notification.setTitle(this.title);
      notification.setBody(this.body);
      notification.setPayloadJson(this.payloadJson);
      notification.setStatus(this.status);
      notification.setScheduledAt(this.scheduledAt);
      notification.setDeliveredAt(this.deliveredAt);
      notification.setReadAt(this.readAt);
      return notification;
    }
  }
}
