package app.vetra.ai.service;

import app.vetra.ai.dto.AIScanResponse;
import app.vetra.ai.entity.AIScan;
import app.vetra.ai.entity.AIScanStatus;
import app.vetra.ai.event.AIScanEscalatedEvent;
import app.vetra.ai.repository.AIScanRepository;
import app.vetra.auth.repository.UserRepository;
import app.vetra.disease.service.AIScanDiseaseReportService;
import app.vetra.infrastructure.exception.BusinessRuleException;
import app.vetra.infrastructure.exception.ResourceNotFoundException;
import app.vetra.infrastructure.exception.UnauthorizedResourceAccessException;
import app.vetra.auth.repository.ParaVetProfileRepository;
import app.vetra.infrastructure.persistence.entity.FarmerProfile;
import app.vetra.infrastructure.persistence.entity.ParaVetProfile;
import app.vetra.infrastructure.persistence.enums.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.enums.UserRole;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Para-vet triage: a para-vet checks a farmer's AI scan in the field and escalates it to a vet
 * (filing a SUSPECTED case). Closing a scan uses the shared reject flow in {@link AIScanService}.
 */
@Service
public class AIScanTriageService {

  private static final Logger log = LoggerFactory.getLogger(AIScanTriageService.class);

  private final AIScanRepository aiScanRepository;
  private final UserRepository userRepository;
  private final AIScanDiseaseReportService aiScanDiseaseReportService;
  private final ApplicationEventPublisher eventPublisher;
  private final ParaVetProfileRepository paraVetProfileRepository;

  public AIScanTriageService(
      AIScanRepository aiScanRepository,
      UserRepository userRepository,
      AIScanDiseaseReportService aiScanDiseaseReportService,
      ApplicationEventPublisher eventPublisher,
      ParaVetProfileRepository paraVetProfileRepository) {
    this.paraVetProfileRepository = paraVetProfileRepository;
    this.aiScanRepository = aiScanRepository;
    this.userRepository = userRepository;
    this.aiScanDiseaseReportService = aiScanDiseaseReportService;
    this.eventPublisher = eventPublisher;
  }

  /** The para-vet's profile, or null when the user is not a para-vet. */
  @Transactional(readOnly = true)
  public ParaVetProfile paraVetProfile(String userIdentifier) {
    return userRepository
        .findByIdentifier(userIdentifier)
        .filter(u -> u.getRole() == UserRole.PARA_VET)
        .flatMap(u -> paraVetProfileRepository.findByUserId(u.getId()))
        .orElse(null);
  }

  /** Throws unless the user is a para-vet an officer has approved. */
  public void requireApprovedParaVet(String userIdentifier) {
    ParaVetProfile p = paraVetProfile(userIdentifier);
    if (p == null) {
      throw new UnauthorizedResourceAccessException("Only para-vets can do this", "AUTH_006");
    }
    if (p.getVerificationStatus() != VerificationStatus.VERIFIED) {
      throw new UnauthorizedResourceAccessException(
          "Your para-vet account is waiting for approval by the district office", "AUTH_007");
    }
  }

  /** Scans from the para-vet's own district (all scans if their district is not set). */
  @Transactional(readOnly = true)
  public Page<AIScanResponse> listForParaVet(ParaVetProfile p, Pageable pageable) {
    Page<AIScan> page =
        p.getDistrict() == null || p.getDistrict().isBlank()
            ? aiScanRepository.findAll(pageable)
            : aiScanRepository.findByFarmDistrict(p.getDistrict().trim(), pageable);
    return page.map(AIScanResponse::fromEntity);
  }

  /** Sends a completed scan to a vet; only a vet can then confirm or reject it. */
  @Transactional
  public AIScanResponse escalateScan(String userIdentifier, UUID scanId, String fieldNotes) {
    requireApprovedParaVet(userIdentifier);
    User user =
        userRepository
            .findByIdentifier(userIdentifier)
            .orElseThrow(() -> new ResourceNotFoundException("User not found", "USER_004"));
    AIScan scan =
        aiScanRepository
            .findById(scanId)
            .orElseThrow(
                () -> new ResourceNotFoundException("AI Diagnostic scan not found with ID: " + scanId, "AI_001"));
    if (scan.getStatus() != AIScanStatus.COMPLETED) {
      throw new BusinessRuleException("Only a finished, unreviewed scan can be escalated", "AI_009");
    }

    String notes = fieldNotes != null && !fieldNotes.isBlank() ? fieldNotes.trim() : null;
    scan.setStatus(AIScanStatus.ESCALATED);
    scan.setTriagedBy(user);
    scan.setTriagedAt(Instant.now());
    scan.setTriageNotes(notes);
    scan = aiScanRepository.save(scan);

    aiScanDiseaseReportService.createSuspectedReport(scan, user, notes);

    FarmerProfile farmer = scan.getAnimal().getFarmer();
    eventPublisher.publishEvent(
        new AIScanEscalatedEvent(
            scan.getId(),
            scan.getUploadedBy().getId(),
            scan.getAnimal().getAnimalName(),
            scan.getDiagnosis(),
            farmer != null ? farmer.getLatitude() : null,
            farmer != null ? farmer.getLongitude() : null));
    log.info("AI Scan ESCALATED scanId={} by paraVetId={}", scan.getId(), user.getId());
    return AIScanResponse.fromEntity(scan);
  }
}
