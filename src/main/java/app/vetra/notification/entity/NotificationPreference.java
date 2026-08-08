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

/** End-user channel opt-in/opt-out notification preferences. */
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

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public boolean isAppointmentNotifications() {
    return appointmentNotifications;
  }

  public void setAppointmentNotifications(boolean appointmentNotifications) {
    this.appointmentNotifications = appointmentNotifications;
  }

  public boolean isVaccinationNotifications() {
    return vaccinationNotifications;
  }

  public void setVaccinationNotifications(boolean vaccinationNotifications) {
    this.vaccinationNotifications = vaccinationNotifications;
  }

  public boolean isAiNotifications() {
    return aiNotifications;
  }

  public void setAiNotifications(boolean aiNotifications) {
    this.aiNotifications = aiNotifications;
  }

  public boolean isOutbreakNotifications() {
    return outbreakNotifications;
  }

  public void setOutbreakNotifications(boolean outbreakNotifications) {
    this.outbreakNotifications = outbreakNotifications;
  }

  public boolean isMarketingNotifications() {
    return marketingNotifications;
  }

  public void setMarketingNotifications(boolean marketingNotifications) {
    this.marketingNotifications = marketingNotifications;
  }

  public static NotificationPreferenceBuilder builder() {
    return new NotificationPreferenceBuilder();
  }

  public static class NotificationPreferenceBuilder {
    private User user;
    private boolean appointmentNotifications = true;
    private boolean vaccinationNotifications = true;
    private boolean aiNotifications = true;
    private boolean outbreakNotifications = true;
    private boolean marketingNotifications = false;

    public NotificationPreferenceBuilder user(User user) {
      this.user = user;
      return this;
    }

    public NotificationPreferenceBuilder appointmentNotifications(boolean appointmentNotifications) {
      this.appointmentNotifications = appointmentNotifications;
      return this;
    }

    public NotificationPreferenceBuilder vaccinationNotifications(boolean vaccinationNotifications) {
      this.vaccinationNotifications = vaccinationNotifications;
      return this;
    }

    public NotificationPreferenceBuilder aiNotifications(boolean aiNotifications) {
      this.aiNotifications = aiNotifications;
      return this;
    }

    public NotificationPreferenceBuilder outbreakNotifications(boolean outbreakNotifications) {
      this.outbreakNotifications = outbreakNotifications;
      return this;
    }

    public NotificationPreferenceBuilder marketingNotifications(boolean marketingNotifications) {
      this.marketingNotifications = marketingNotifications;
      return this;
    }

    public NotificationPreference build() {
      NotificationPreference pref = new NotificationPreference();
      pref.setUser(this.user);
      pref.setAppointmentNotifications(this.appointmentNotifications);
      pref.setVaccinationNotifications(this.vaccinationNotifications);
      pref.setAiNotifications(this.aiNotifications);
      pref.setOutbreakNotifications(this.outbreakNotifications);
      pref.setMarketingNotifications(this.marketingNotifications);
      return pref;
    }
  }
}
