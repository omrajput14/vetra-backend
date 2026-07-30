package app.vetra.auth.service;

import app.vetra.auth.dto.AuthResponse;
import app.vetra.auth.dto.ChangePasswordRequest;
import app.vetra.auth.dto.FarmerRegisterRequest;
import app.vetra.auth.dto.LoginRequest;
import app.vetra.auth.dto.RefreshTokenRequest;
import app.vetra.auth.dto.UpdateProfileRequest;
import app.vetra.auth.dto.UserProfileDto;
import app.vetra.auth.dto.VetRegisterRequest;
import app.vetra.auth.repository.FarmerProfileRepository;
import app.vetra.auth.repository.UserRepository;
import app.vetra.auth.repository.VetProfileRepository;
import app.vetra.infrastructure.exception.ConflictException;
import app.vetra.infrastructure.exception.ResourceNotFoundException;
import app.vetra.infrastructure.exception.UnauthorizedResourceAccessException;
import app.vetra.infrastructure.persistence.entity.FarmerProfile;
import app.vetra.infrastructure.persistence.entity.RefreshToken;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.infrastructure.persistence.enums.UserRole;
import app.vetra.infrastructure.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core authentication service handling registration, login, token refresh, and profile updates.
 */
@Service
public class AuthService {

  private final UserRepository userRepository;
  private final FarmerProfileRepository farmerProfileRepository;
  private final VetProfileRepository vetProfileRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;
  private final RefreshTokenService refreshTokenService;

  /** Constructor injection. */
  public AuthService(
      UserRepository userRepository,
      FarmerProfileRepository farmerProfileRepository,
      VetProfileRepository vetProfileRepository,
      PasswordEncoder passwordEncoder,
      JwtUtil jwtUtil,
      RefreshTokenService refreshTokenService) {
    this.userRepository = userRepository;
    this.farmerProfileRepository = farmerProfileRepository;
    this.vetProfileRepository = vetProfileRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtUtil = jwtUtil;
    this.refreshTokenService = refreshTokenService;
  }

  /** Registers a farmer user and profile. */
  @Transactional
  public AuthResponse registerFarmer(FarmerRegisterRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new ConflictException("Email is already registered", "USER_001");
    }
    if (request.phone() != null && userRepository.existsByPhone(request.phone())) {
      throw new ConflictException("Phone number is already registered", "USER_002");
    }

    User user = User.builder()
        .email(request.email())
        .phone(request.phone())
        .passwordHash(passwordEncoder.encode(request.password()))
        .role(UserRole.FARMER)
        .isActive(true)
        .build();

    user = userRepository.save(user);

    FarmerProfile profile = FarmerProfile.builder()
        .user(user)
        .fullName(request.fullName())
        .farmName(request.farmName())
        .village(request.village())
        .district(request.district())
        .state(request.state())
        .latitude(request.latitude())
        .longitude(request.longitude())
        .animalCount(request.animalCount())
        .build();

    farmerProfileRepository.save(profile);

    return createAuthResponse(user, mapFarmerProfileToDto(user, profile));
  }

  /** Registers a veterinarian user and profile. */
  @Transactional
  public AuthResponse registerVet(VetRegisterRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new ConflictException("Email is already registered", "USER_001");
    }
    if (request.phone() != null && !request.phone().isBlank() && userRepository.existsByPhone(request.phone())) {
      throw new ConflictException("Phone number is already registered", "USER_002");
    }
    if (vetProfileRepository.existsByRegistrationNumber(request.registrationNumber())) {
      throw new ConflictException("Registration number is already registered", "USER_003");
    }

    User user = User.builder()
        .email(request.email())
        .phone(request.phone())
        .passwordHash(passwordEncoder.encode(request.password()))
        .role(UserRole.VETERINARIAN)
        .isActive(true)
        .build();

    user = userRepository.save(user);

    VetProfile profile = VetProfile.builder()
        .user(user)
        .fullName(request.fullName())
        .registrationNumber(request.registrationNumber())
        .qualification(request.qualification())
        .specialization(request.specialization())
        .clinicName(request.clinicName())
        .yearsExperience(request.yearsExperience())
        .latitude(request.latitude())
        .longitude(request.longitude())
        .isAvailable(true)
        .build();

    vetProfileRepository.save(profile);

    return createAuthResponse(user, mapVetProfileToDto(user, profile));
  }

  /** Authenticates farmer user login. */
  @Transactional
  public AuthResponse loginFarmer(LoginRequest request) {
    User user = authenticateUser(request, UserRole.FARMER);
    FarmerProfile profile = farmerProfileRepository.findByUser(user)
        .orElseThrow(() -> new ResourceNotFoundException("Farmer profile missing for user", "USER_004"));
    return createAuthResponse(user, mapFarmerProfileToDto(user, profile));
  }

  /** Authenticates veterinarian user login. */
  @Transactional
  public AuthResponse loginVet(LoginRequest request) {
    User user = authenticateUser(request, UserRole.VETERINARIAN);
    VetProfile profile = vetProfileRepository.findByUser(user)
        .orElseThrow(() -> new ResourceNotFoundException("Veterinarian profile missing for user", "USER_004"));
    return createAuthResponse(user, mapVetProfileToDto(user, profile));
  }

  /** Refreshes access token and rotates refresh token using raw token string. */
  @Transactional
  public AuthResponse refreshToken(RefreshTokenRequest request) {
    RefreshToken refreshToken = refreshTokenService.findByRawToken(request.refreshToken())
        .map(refreshTokenService::verifyExpiration)
        .orElseThrow(() -> new UnauthorizedResourceAccessException("Invalid or expired refresh token", "AUTH_004"));

    User user = refreshToken.getUser();
    UserProfileDto profileDto = getCurrentUserProfileDto(user);
    return createAuthResponse(user, profileDto);
  }

  /** Revokes session on logout using raw token string. */
  @Transactional
  public void logout(String refreshToken) {
    refreshTokenService.revokeToken(refreshToken);
  }

  /** Changes password for user and revokes all active sessions across all devices. */
  @Transactional
  public void changePassword(String identifier, ChangePasswordRequest request) {
    User user = userRepository.findByIdentifier(identifier)
        .orElseThrow(() -> new ResourceNotFoundException("User not found", "USER_004"));

    if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
      throw new UnauthorizedResourceAccessException("Current password does not match", "AUTH_001");
    }

    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    userRepository.save(user);

    refreshTokenService.revokeAllUserTokens(user);
  }

  /** Updates active user profile and returns refreshed UserProfileDto. */
  @Transactional
  public UserProfileDto updateUserProfile(String currentUserIdentifier, UpdateProfileRequest request) {
    User user = userRepository.findByIdentifier(currentUserIdentifier)
        .orElseThrow(() -> new ResourceNotFoundException("User not found", "USER_004"));

    if (request.phone() != null && !request.phone().isBlank() && !request.phone().equals(user.getPhone())) {
      user.setPhone(request.phone());
    }

    if (user.getRole() == UserRole.FARMER) {
      updateFarmerProfile(user, request);
    } else if (user.getRole() == UserRole.VETERINARIAN) {
      updateVetProfile(user, request);
    }

    user = userRepository.save(user);
    return getCurrentUserProfileDto(user);
  }

  private void updateFarmerProfile(User user, UpdateProfileRequest request) {
    FarmerProfile profile = farmerProfileRepository.findByUser(user)
        .orElseGet(() -> FarmerProfile.builder().user(user).build());
    if (request.fullName() != null && !request.fullName().isBlank()) {
      profile.setFullName(request.fullName());
    }
    if (request.farmName() != null) {
      profile.setFarmName(request.farmName());
    }
    if (request.village() != null) {
      profile.setVillage(request.village());
    }
    if (request.district() != null) {
      profile.setDistrict(request.district());
    }
    if (request.state() != null) {
      profile.setState(request.state());
    }
    farmerProfileRepository.save(profile);
  }

  private void updateVetProfile(User user, UpdateProfileRequest request) {
    VetProfile profile = vetProfileRepository.findByUser(user)
        .orElseGet(() -> VetProfile.builder().user(user).registrationNumber("VET-" + System.currentTimeMillis()).build());
    if (request.fullName() != null && !request.fullName().isBlank()) {
      profile.setFullName(request.fullName());
    }
    if (request.clinicName() != null) {
      profile.setClinicName(request.clinicName());
    }
    if (request.specialization() != null) {
      profile.setSpecialization(request.specialization());
    }
    if (request.qualification() != null) {
      profile.setQualification(request.qualification());
    }
    if (request.yearsExperience() != null) {
      profile.setYearsExperience(request.yearsExperience());
    }
    vetProfileRepository.save(profile);
  }

  /** Retrieves user profile DTO for authenticated user. */
  @Transactional(readOnly = true)
  public UserProfileDto getCurrentUserProfileDtoByIdentifier(String identifier) {
    User user = userRepository.findByIdentifier(identifier)
        .orElseThrow(() -> new ResourceNotFoundException("User not found", "USER_004"));
    return getCurrentUserProfileDto(user);
  }

  private User authenticateUser(LoginRequest request, UserRole expectedRole) {
    User user = userRepository.findByIdentifier(request.identifier())
        .orElseThrow(() -> new UnauthorizedResourceAccessException("Invalid credentials", "AUTH_001"));

    if (user.getRole() != expectedRole) {
      throw new UnauthorizedResourceAccessException("Access denied for role: " + user.getRole(), "AUTH_006");
    }

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new UnauthorizedResourceAccessException("Invalid credentials", "AUTH_001");
    }

    return user;
  }

  private AuthResponse createAuthResponse(User user, UserProfileDto profileDto) {
    String accessToken = jwtUtil.generateAccessToken(
        user.getEmail() != null ? user.getEmail() : user.getPhone(),
        user.getRole().name());
    String rawRefreshToken = refreshTokenService.createRefreshToken(user);

    return new AuthResponse(
        accessToken,
        rawRefreshToken,
        "Bearer",
        jwtUtil.getExpirationMs() / 1000,
        profileDto
    );
  }

  /** Retrieves user profile DTO from user entity based on role. */
  public UserProfileDto getCurrentUserProfileDto(User user) {
    if (user.getRole() == UserRole.FARMER) {
      FarmerProfile profile = farmerProfileRepository.findByUser(user)
          .orElse(null);
      return mapFarmerProfileToDto(user, profile);
    } else if (user.getRole() == UserRole.VETERINARIAN) {
      VetProfile profile = vetProfileRepository.findByUser(user)
          .orElse(null);
      return mapVetProfileToDto(user, profile);
    }
    return new UserProfileDto(
        user.getId(), user.getEmail(), user.getPhone(), user.getRole(), user.isActive(),
        null, null, null, null, null, null, null, null, null, null, null, null, null, null
    );
  }

  /** Retrieves list of all registered veterinarians for directory and booking pickers. */
  @Transactional(readOnly = true)
  public java.util.List<app.vetra.auth.dto.VetSummaryDto> listVeterinarians() {
    return vetProfileRepository.findAll().stream()
        .map(app.vetra.auth.dto.VetSummaryDto::fromEntity)
        .toList();
  }

  private UserProfileDto mapFarmerProfileToDto(User user, FarmerProfile p) {
    return new UserProfileDto(
        user.getId(), user.getEmail(), user.getPhone(), user.getRole(), user.isActive(),
        p != null ? p.getFullName() : null,
        p != null ? p.getFarmName() : null,
        p != null ? p.getVillage() : null,
        p != null ? p.getDistrict() : null,
        p != null ? p.getState() : null,
        p != null ? p.getLatitude() : null,
        p != null ? p.getLongitude() : null,
        p != null ? p.getAnimalCount() : null,
        null, null, null, null, null, null
    );
  }

  private UserProfileDto mapVetProfileToDto(User user, VetProfile v) {
    return new UserProfileDto(
        user.getId(), user.getEmail(), user.getPhone(), user.getRole(), user.isActive(),
        v != null ? v.getFullName() : null,
        null, null, null, null,
        v != null ? v.getLatitude() : null,
        v != null ? v.getLongitude() : null,
        null,
        v != null ? v.getRegistrationNumber() : null,
        v != null ? v.getQualification() : null,
        v != null ? v.getSpecialization() : null,
        v != null ? v.getClinicName() : null,
        v != null ? v.getYearsExperience() : null,
        v != null ? v.isAvailable() : null
    );
  }
}
