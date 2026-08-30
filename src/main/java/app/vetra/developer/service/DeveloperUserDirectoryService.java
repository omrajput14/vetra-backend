package app.vetra.developer.service;

import app.vetra.auth.repository.FarmerProfileRepository;
import app.vetra.auth.repository.RefreshTokenRepository;
import app.vetra.auth.repository.UserRepository;
import app.vetra.auth.repository.VetProfileRepository;
import app.vetra.developer.dto.DeveloperUserDto;
import app.vetra.developer.dto.DeveloperUserPageResponse;
import app.vetra.infrastructure.persistence.entity.FarmerProfile;
import app.vetra.infrastructure.persistence.entity.RefreshToken;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.infrastructure.persistence.enums.UserRole;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for querying and mapping user directory entries without exposing credentials. */
@Service
public class DeveloperUserDirectoryService {

  private final UserRepository userRepository;
  private final FarmerProfileRepository farmerProfileRepository;
  private final VetProfileRepository vetProfileRepository;
  private final RefreshTokenRepository refreshTokenRepository;

  public DeveloperUserDirectoryService(
      UserRepository userRepository,
      FarmerProfileRepository farmerProfileRepository,
      VetProfileRepository vetProfileRepository,
      RefreshTokenRepository refreshTokenRepository) {
    this.userRepository = userRepository;
    this.farmerProfileRepository = farmerProfileRepository;
    this.vetProfileRepository = vetProfileRepository;
    this.refreshTokenRepository = refreshTokenRepository;
  }

  @Transactional(readOnly = true)
  public DeveloperUserPageResponse getUsers(
      UserRole role, Boolean isActive, String search, int page, int size) {
    Pageable pageable =
        PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "createdAt"));

    Page<User> userPage;
    if (role != null && isActive != null) {
      userPage = userRepository.findByRoleAndIsActive(role, isActive, pageable);
    } else if (role != null) {
      userPage = userRepository.findByRole(role, pageable);
    } else if (isActive != null) {
      userPage = userRepository.findByIsActive(isActive, pageable);
    } else {
      userPage = userRepository.findAll(pageable);
    }

    List<DeveloperUserDto> dtos =
        userPage.getContent().stream()
            .map(this::mapToDeveloperUserDto)
            .filter(dto -> matchesSearch(dto, search))
            .collect(Collectors.toList());

    return new DeveloperUserPageResponse(
        dtos,
        userPage.getNumber(),
        userPage.getSize(),
        userPage.getTotalElements(),
        userPage.getTotalPages(),
        userPage.hasNext(),
        userPage.hasPrevious());
  }

  private boolean matchesSearch(DeveloperUserDto dto, String search) {
    if (search == null || search.isBlank()) {
      return true;
    }
    String query = search.trim().toLowerCase();
    return (dto.email() != null && dto.email().toLowerCase().contains(query))
        || (dto.phone() != null && dto.phone().toLowerCase().contains(query))
        || (dto.profileName() != null && dto.profileName().toLowerCase().contains(query))
        || (dto.locationSummary() != null && dto.locationSummary().toLowerCase().contains(query));
  }

  public DeveloperUserDto mapToDeveloperUserDto(User user) {
    String profileName = null;
    String farmOrClinicName = null;
    String locationSummary = null;
    String vetRegistrationNumber = null;
    String vetVerificationStatus = null;
    Integer vetYearsExperience = null;
    String vetSpecialization = null;
    Boolean vetIsAvailable = null;
    Boolean vetEmergencyAvailable = null;
    Integer farmerAnimalCount = null;

    if (user.getRole() == UserRole.FARMER) {
      Optional<FarmerProfile> farmerOpt = farmerProfileRepository.findByUser(user);
      if (farmerOpt.isPresent()) {
        FarmerProfile f = farmerOpt.get();
        profileName = f.getFullName();
        farmOrClinicName = f.getFarmName();
        farmerAnimalCount = f.getAnimalCount();
        locationSummary = formatLocation(f.getVillage(), f.getTaluka(), f.getDistrict(), f.getState());
      }
    } else if (user.getRole() == UserRole.VETERINARIAN) {
      Optional<VetProfile> vetOpt = vetProfileRepository.findByUser(user);
      if (vetOpt.isPresent()) {
        VetProfile v = vetOpt.get();
        profileName = v.getFullName();
        farmOrClinicName = v.getClinicName();
        vetRegistrationNumber = v.getRegistrationNumber();
        vetVerificationStatus = v.getVerificationStatus() != null ? v.getVerificationStatus().name() : "PENDING";
        vetYearsExperience = v.getYearsExperience();
        vetSpecialization = v.getSpecialization();
        vetIsAvailable = v.isAvailable();
        vetEmergencyAvailable = v.isEmergencyAvailable();
        locationSummary = formatLocation(v.getVillage(), v.getTaluka(), v.getDistrict(), v.getState());
      }
    } else if (user.getRole() == UserRole.GOVERNMENT_OFFICER) {
      profileName = "Government Surveillance Officer";
      farmOrClinicName = "State Department of Animal Husbandry";
    } else if (user.getRole() == UserRole.ADMINISTRATOR) {
      profileName = "Platform Administrator";
      farmOrClinicName = "VETRA Systems";
    }

    Instant lastLoginAt =
        refreshTokenRepository
            .findFirstByUserOrderByCreatedAtDesc(user)
            .map(RefreshToken::getCreatedAt)
            .orElse(null);

    return new DeveloperUserDto(
        user.getId(),
        user.getEmail(),
        user.getPhone(),
        user.getRole(),
        user.isActive(),
        user.getPreferredLanguage(),
        user.getCreatedAt(),
        user.getUpdatedAt(),
        lastLoginAt,
        profileName,
        farmOrClinicName,
        locationSummary,
        vetRegistrationNumber,
        vetVerificationStatus,
        vetYearsExperience,
        vetSpecialization,
        vetIsAvailable,
        vetEmergencyAvailable,
        farmerAnimalCount);
  }

  private String formatLocation(String village, String taluka, String district, String state) {
    List<String> parts = new ArrayList<>();
    if (village != null && !village.isBlank()) {
      parts.add(village);
    }
    if (taluka != null && !taluka.isBlank()) {
      parts.add(taluka);
    }
    if (district != null && !district.isBlank()) {
      parts.add(district);
    }
    if (state != null && !state.isBlank()) {
      parts.add(state);
    }
    return parts.isEmpty() ? "Location Unspecified" : String.join(", ", parts);
  }
}
