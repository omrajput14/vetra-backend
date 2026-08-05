package app.vetra.notification.entity;

import app.vetra.infrastructure.persistence.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Reusable notification template with placeholder support. */
@Entity
@Table(name = "notification_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplate extends BaseEntity {

  @Column(name = "code", nullable = false, unique = true, length = 64)
  private String code;

  @Column(name = "name", nullable = false, length = 128)
  private String name;

  @Column(name = "subject_template", nullable = false, length = 256)
  private String subjectTemplate;

  @Column(name = "body_template", nullable = false, columnDefinition = "TEXT")
  private String bodyTemplate;

  @Enumerated(EnumType.STRING)
  @Column(name = "default_channel", nullable = false, length = 32)
  @Builder.Default
  private NotificationChannel defaultChannel = NotificationChannel.PUSH;
}
