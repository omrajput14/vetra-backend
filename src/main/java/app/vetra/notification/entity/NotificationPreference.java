package app.vetra.notification.entity;

import app.vetra.infrastructure.persistence.entity.BaseEntity;
import app.vetra.infrastructure.persistence.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * End-user channel opt-in/opt-out notification preferences.
 */
@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference extends BaseEntity {

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Column(name = "appointment_notifications", nullable = false)
  @Builder.Default
  private boolean appointmentNotifications = true;

  @Column(name = "vaccination_notifications", nullable = false)
  @Builder.Default
  private boolean vaccinationNotifications = true;

  @Column(name = "ai_notifications", nullable = false)
  @Builder.Default
  private boolean aiNotifications = true;

  @Column(name = "outbreak_notifications", nullable = false)
  @Builder.Default
  private boolean outbreakNotifications = true;

  @Column(name = "marketing_notifications", nullable = false)
  @Builder.Default
  private boolean marketingNotifications = false;
}
