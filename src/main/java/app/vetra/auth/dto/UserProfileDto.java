package app.vetra.auth.dto;

import app.vetra.infrastructure.persistence.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;

/** Unified user profile response DTO combining User and role profile details. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserProfileDto(
    UUID id,
    String email,
    String phone,
    UserRole role,
    boolean isActive,
    String preferredLanguage,
    String fullName,
    String farmName,
    String village,
    String district,
    String state,
    Double latitude,
    Double longitude,
    Integer animalCount,
    String registrationNumber,
    String qualification,
    String specialization,
    String clinicName,
    Integer yearsExperience,
    Boolean isAvailable) {

  @SuppressWarnings("checkstyle:ParameterNumber")
  public UserProfileDto(
      UUID id,
      String email,
      String phone,
      UserRole role,
      boolean isActive,
      String fullName,
      String farmName,
      String village,
      String district,
      String state,
      Double latitude,
      Double longitude,
      Integer animalCount,
      String registrationNumber,
      String qualification,
      String specialization,
      String clinicName,
      Integer yearsExperience,
      Boolean isAvailable) {
    this(
        id,
        email,
        phone,
        role,
        isActive,
        "en",
        fullName,
        farmName,
        village,
        district,
        state,
        latitude,
        longitude,
        animalCount,
        registrationNumber,
        qualification,
        specialization,
        clinicName,
        yearsExperience,
        isAvailable);
  }
}
