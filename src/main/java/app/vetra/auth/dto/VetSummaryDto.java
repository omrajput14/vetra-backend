package app.vetra.auth.dto;

import app.vetra.infrastructure.persistence.entity.VetProfile;
import java.util.UUID;

/** Summary DTO for veterinarian directory and nearby vet selection. */
public record VetSummaryDto(
    UUID id,
    String fullName,
    String registrationNumber,
    String qualification,
    String specialization,
    String clinicName,
    Integer yearsExperience,
    Boolean isAvailable,
    Double latitude,
    Double longitude,
    String phone,
    String email) {

  /** Converts a VetProfile entity to VetSummaryDto. */
  public static VetSummaryDto fromEntity(VetProfile vet) {
    return new VetSummaryDto(
        vet.getId(),
        vet.getFullName(),
        vet.getRegistrationNumber(),
        vet.getQualification(),
        vet.getSpecialization(),
        vet.getClinicName(),
        vet.getYearsExperience(),
        vet.isAvailable(),
        vet.getLatitude(),
        vet.getLongitude(),
        vet.getUser() != null ? vet.getUser().getPhone() : null,
        vet.getUser() != null ? vet.getUser().getEmail() : null);
  }
}
