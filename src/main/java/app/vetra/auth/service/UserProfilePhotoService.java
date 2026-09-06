package app.vetra.auth.service;

import app.vetra.auth.dto.UserProfileDto;
import app.vetra.infrastructure.exception.ResourceNotFoundException;
import app.vetra.infrastructure.persistence.entity.FarmerProfile;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.infrastructure.persistence.enums.UserRole;
import app.vetra.auth.repository.FarmerProfileRepository;
import app.vetra.auth.repository.UserRepository;
import app.vetra.auth.repository.VetProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service dedicated to updating and retrieving user profile photos.
 * Keeps profile media operations isolated from general authentication routines.
 */
@Service
public class UserProfilePhotoService {

  private final UserRepository userRepository;
  private final FarmerProfileRepository farmerProfileRepository;
  private final VetProfileRepository vetProfileRepository;
  private final AuthService authService;

  public UserProfilePhotoService(
      UserRepository userRepository,
      FarmerProfileRepository farmerProfileRepository,
      VetProfileRepository vetProfileRepository,
      AuthService authService) {
    this.userRepository = userRepository;
    this.farmerProfileRepository = farmerProfileRepository;
    this.vetProfileRepository = vetProfileRepository;
    this.authService = authService;
  }

  /**
   * Updates profile photo URL for the authenticated user (farmer or veterinarian).
   */
  @Transactional
  public UserProfileDto updateUserProfilePhoto(String currentUserIdentifier, String photoUrl) {
    User user =
        userRepository
            .findByIdentifier(currentUserIdentifier)
            .orElseThrow(() -> new ResourceNotFoundException("User not found", "USER_004"));

    if (user.getRole() == UserRole.FARMER) {
      FarmerProfile p =
          farmerProfileRepository
              .findByUser(user)
              .orElseGet(() -> FarmerProfile.builder().user(user).build());
      p.setProfilePhotoUrl(photoUrl);
      farmerProfileRepository.save(p);
    } else if (user.getRole() == UserRole.VETERINARIAN) {
      VetProfile p =
          vetProfileRepository
              .findByUser(user)
              .orElseGet(
                  () ->
                      VetProfile.builder()
                          .user(user)
                          .registrationNumber("VET-" + System.currentTimeMillis())
                          .build());
      p.setProfilePhotoUrl(photoUrl);
      vetProfileRepository.save(p);
    }

    return authService.getCurrentUserProfileDtoByIdentifier(currentUserIdentifier);
  }

  /**
   * Removes profile photo URL for the authenticated user.
   */
  @Transactional
  public UserProfileDto removeUserProfilePhoto(String currentUserIdentifier) {
    return updateUserProfilePhoto(currentUserIdentifier, null);
  }

  /**
   * Retrieves User entity by identifier.
   */
  @Transactional(readOnly = true)
  public User getUserByIdentifier(String currentUserIdentifier) {
    return userRepository
        .findByIdentifier(currentUserIdentifier)
        .orElseThrow(() -> new ResourceNotFoundException("User not found", "USER_004"));
  }
}
