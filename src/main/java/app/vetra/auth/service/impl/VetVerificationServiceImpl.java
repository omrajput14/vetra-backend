package app.vetra.auth.service.impl;

import app.vetra.auth.repository.VetProfileRepository;
import app.vetra.auth.service.VetVerificationService;
import app.vetra.infrastructure.exception.ResourceNotFoundException;
import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.infrastructure.persistence.enums.VerificationStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementation of veterinarian verification lifecycle and verification rules. */
@Service
public class VetVerificationServiceImpl implements VetVerificationService {

  private final VetProfileRepository vetProfileRepository;

  public VetVerificationServiceImpl(VetProfileRepository vetProfileRepository) {
    this.vetProfileRepository = vetProfileRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isVerified(UUID vetProfileId) {
    return vetProfileRepository
        .findById(vetProfileId)
        .map(VetProfile::isVerified)
        .orElse(false);
  }

  @Override
  @Transactional
  public VetProfile updateVerificationStatus(UUID vetProfileId, VerificationStatus newStatus) {
    VetProfile profile =
        vetProfileRepository
            .findById(vetProfileId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Veterinarian profile not found with ID: " + vetProfileId, "VET_001"));

    profile.setVerificationStatus(newStatus);
    return vetProfileRepository.save(profile);
  }

  @Override
  @Transactional
  public VetProfile verifyVeterinarian(UUID vetProfileId) {
    return updateVerificationStatus(vetProfileId, VerificationStatus.VERIFIED);
  }

  @Override
  @Transactional
  public VetProfile rejectVeterinarian(UUID vetProfileId, String reason) {
    return updateVerificationStatus(vetProfileId, VerificationStatus.REJECTED);
  }

  @Override
  @Transactional(readOnly = true)
  public List<VetProfile> getVerifiedVeterinarians() {
    return vetProfileRepository.findByVerificationStatus(VerificationStatus.VERIFIED);
  }
}
