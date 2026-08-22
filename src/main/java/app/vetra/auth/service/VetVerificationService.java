package app.vetra.auth.service;

import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.infrastructure.persistence.enums.VerificationStatus;
import java.util.List;
import java.util.UUID;

/** Service interface managing veterinarian professional verification lifecycle and rules. */
public interface VetVerificationService {

  /** Checks if a veterinarian profile exists and is in VERIFIED state. */
  boolean isVerified(UUID vetProfileId);

  /** Updates verification status of a veterinarian profile. */
  VetProfile updateVerificationStatus(UUID vetProfileId, VerificationStatus newStatus);

  /** Approves and verifies a veterinarian profile. */
  VetProfile verifyVeterinarian(UUID vetProfileId);

  /** Rejects a veterinarian profile with reason. */
  VetProfile rejectVeterinarian(UUID vetProfileId, String reason);

  /** Retrieves all verified veterinarian profiles for directory and public discovery. */
  List<VetProfile> getVerifiedVeterinarians();
}
