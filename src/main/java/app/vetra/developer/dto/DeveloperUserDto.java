package app.vetra.developer.dto;

import app.vetra.infrastructure.persistence.enums.UserRole;
import java.time.Instant;
import java.util.UUID;

/** Non-sensitive user representation for the Developer Console user directory. */
public record DeveloperUserDto(
    UUID id,
    String email,
    String phone,
    UserRole role,
    boolean isActive,
    String preferredLanguage,
    Instant createdAt,
    Instant updatedAt,
    Instant lastLoginAt,
    String profileName,
    String farmOrClinicName,
    String locationSummary,
    String vetRegistrationNumber,
    String vetVerificationStatus,
    Integer vetYearsExperience,
    String vetSpecialization,
    Boolean vetIsAvailable,
    Boolean vetEmergencyAvailable,
    Integer farmerAnimalCount) {}
