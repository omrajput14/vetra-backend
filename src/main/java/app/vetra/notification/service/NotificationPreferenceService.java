package app.vetra.notification.service;

import app.vetra.auth.repository.UserRepository;
import app.vetra.infrastructure.exception.ResourceNotFoundException;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.notification.dto.NotificationPreferenceResponse;
import app.vetra.notification.dto.UpdatePreferenceRequest;
import app.vetra.notification.entity.NotificationPreference;
import app.vetra.notification.repository.NotificationPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing user notification preferences and opt-in/opt-out validation.
 */
@Service
public class NotificationPreferenceService {

  private final NotificationPreferenceRepository preferenceRepository;
  private final UserRepository userRepository;

  /** Constructor injection. */
  public NotificationPreferenceService(NotificationPreferenceRepository preferenceRepository, UserRepository userRepository) {
    this.preferenceRepository = preferenceRepository;
    this.userRepository = userRepository;
  }

  /** Retrieves user notification preferences, creating defaults if not yet initialized. */
  @Transactional
  public NotificationPreferenceResponse getPreferences(String userIdentifier) {
    User user = getUserByEmailOrPhone(userIdentifier);
    NotificationPreference pref = preferenceRepository.findByUserId(user.getId())
        .orElseGet(() -> preferenceRepository.save(NotificationPreference.builder().user(user).build()));

    return NotificationPreferenceResponse.fromEntity(pref);
  }

  /** Updates user notification preferences. */
  @Transactional
  public NotificationPreferenceResponse updatePreferences(String userIdentifier, UpdatePreferenceRequest request) {
    User user = getUserByEmailOrPhone(userIdentifier);
    NotificationPreference pref = preferenceRepository.findByUserId(user.getId())
        .orElseGet(() -> NotificationPreference.builder().user(user).build());

    if (request.appointmentNotifications() != null) {
      pref.setAppointmentNotifications(request.appointmentNotifications());
    }
    if (request.vaccinationNotifications() != null) {
      pref.setVaccinationNotifications(request.vaccinationNotifications());
    }
    if (request.aiNotifications() != null) {
      pref.setAiNotifications(request.aiNotifications());
    }
    if (request.outbreakNotifications() != null) {
      pref.setOutbreakNotifications(request.outbreakNotifications());
    }
    if (request.marketingNotifications() != null) {
      pref.setMarketingNotifications(request.marketingNotifications());
    }

    pref = preferenceRepository.save(pref);
    return NotificationPreferenceResponse.fromEntity(pref);
  }

  private User getUserByEmailOrPhone(String identifier) {
    return userRepository.findByEmail(identifier)
        .or(() -> userRepository.findByPhone(identifier))
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + identifier, "USER_004"));
  }
}
