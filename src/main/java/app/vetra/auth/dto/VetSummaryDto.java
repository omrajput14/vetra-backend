package app.vetra.auth.dto;

import app.vetra.infrastructure.persistence.entity.VetProfile;
import java.util.UUID;

/** Summary DTO for veterinarian directory and nearby vet selection. */
public record VetSummaryDto(
    UUID id,
    String name,
    String fullName,
    String registrationNumber,
    String qualification,
    String specialization,
    String clinic,
    String clinicName,
    Integer yearsExperience,
    Boolean isAvailable,
    Boolean emergencyAvailable,
    Double rating,
    Double latitude,
    Double longitude,
    String phoneNumber,
    String phone,
    String email) {

  /** Converts a VetProfile entity to VetSummaryDto. */
  public static VetSummaryDto fromEntity(VetProfile vet) {
    String phone = vet.getUser() != null ? vet.getUser().getPhone() : null;
    String name = vet.getFullName();
    String clinic = vet.getClinicName();
    return new VetSummaryDto(
        vet.getId(),
        name,
        name,
        vet.getRegistrationNumber(),
        vet.getQualification(),
        vet.getSpecialization(),
        clinic,
        clinic,
        vet.getYearsExperience(),
        vet.isAvailable(),
        vet.isEmergencyAvailable(),
        5.0,
        vet.getLatitude(),
        vet.getLongitude(),
        phone,
        phone,
        vet.getUser() != null ? vet.getUser().getEmail() : null);
  }
}

