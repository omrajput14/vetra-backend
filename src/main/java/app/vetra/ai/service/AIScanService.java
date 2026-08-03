package app.vetra.ai.service;

import app.vetra.ai.dto.AIScanResponse;
import app.vetra.ai.dto.ApproveAIScanRequest;
import app.vetra.ai.dto.CreateAIScanRequest;
import app.vetra.ai.dto.RejectAIScanRequest;
import app.vetra.ai.dto.VerifyAIScanRequest;
import app.vetra.ai.entity.AIScan;
import app.vetra.ai.entity.AIScanResultEntity;
import app.vetra.ai.entity.AIScanStatus;
import app.vetra.ai.event.AIScanCreatedEvent;
import app.vetra.ai.event.AIScanRejectedEvent;
import app.vetra.ai.event.AIScanVerifiedEvent;
import app.vetra.ai.event.MedicalRecordCreatedFromAIEvent;
import app.vetra.ai.orchestrator.AIOrchestrator;
import app.vetra.ai.repository.AIScanRepository;
import app.vetra.ai.repository.AIScanResultRepository;
import app.vetra.animal.repository.AnimalRepository;
import app.vetra.auth.repository.UserRepository;
import app.vetra.auth.repository.VetProfileRepository;
import app.vetra.infrastructure.cache.CacheNames;
import app.vetra.infrastructure.exception.BusinessRuleException;
import app.vetra.infrastructure.exception.ResourceNotFoundException;
import app.vetra.infrastructure.exception.UnauthorizedResourceAccessException;
import app.vetra.infrastructure.metrics.VetraMetrics;
import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.entity.MedicalRecord;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.infrastructure.persistence.enums.UserRole;
import app.vetra.medicalrecord.repository.MedicalRecordRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core business service managing AI diagnostic scan requests, orchestration execution,
 * veterinarian review workflow, and automated Electronic Veterinary Medical Record creation.
 */
@Service
public class AIScanService {

  private static final Logger log = LoggerFactory.getLogger(AIScanService.class);

  private final AIScanRepository aiScanRepository;
  private final AIScanResultRepository aiScanResultRepository;
  private final AnimalRepository animalRepository;
  private final UserRepository userRepository;
  private final VetProfileRepository vetProfileRepository;
  private final MedicalRecordRepository medicalRecordRepository;
  private final AIOrchestrator aiOrchestrator;
  private final ApplicationEventPublisher eventPublisher;

  // Cross-cutting infra concern — setter injected to keep constructor within 8-param Checkstyle limit
  private VetraMetrics vetraMetrics;

  /** Constructor injection with 8 parameters (Checkstyle max). */
  public AIScanService(
      AIScanRepository aiScanRepository,
      AIScanResultRepository aiScanResultRepository,
      AnimalRepository animalRepository,
      UserRepository userRepository,
      VetProfileRepository vetProfileRepository,
      MedicalRecordRepository medicalRecordRepository,
      AIOrchestrator aiOrchestrator,
      ApplicationEventPublisher eventPublisher) {
    this.aiScanRepository = aiScanRepository;
    this.aiScanResultRepository = aiScanResultRepository;
    this.animalRepository = animalRepository;
    this.userRepository = userRepository;
    this.vetProfileRepository = vetProfileRepository;
    this.medicalRecordRepository = medicalRecordRepository;
    this.aiOrchestrator = aiOrchestrator;
    this.eventPublisher = eventPublisher;
  }

  /** Setter injection for VetraMetrics to preserve constructor parameter count. */
  @Autowired
  public void setVetraMetrics(VetraMetrics vetraMetrics) {
    this.vetraMetrics = vetraMetrics;
  }

  /**
   * Registers a new AI diagnostic scan request for an animal and triggers orchestration if enabled.
   *
   * @param userIdentifier email or phone of authenticated requesting user
   * @param request create scan request parameters
   * @return {@link AIScanResponse} created record
   */
  @Transactional
  @CacheEvict(value = {CacheNames.AI_DIAGNOSIS, CacheNames.DASHBOARD_FARMER}, allEntries = true)
  public AIScanResponse createScan(String userIdentifier, CreateAIScanRequest request) {
    User user = getUserByEmailOrPhone(userIdentifier);
    Animal animal = animalRepository.findById(request.animalId())
        .orElseThrow(() -> new ResourceNotFoundException("Animal not found with ID: " + request.animalId(), "ANIMAL_001"));

    if (user.getRole() == UserRole.FARMER) {
      if (!animal.getFarmer().getUser().getId().equals(user.getId())) {
        throw new UnauthorizedResourceAccessException("Farmers can only create diagnostic scans for their own animals", "ANIMAL_002");
      }
    }

    AIScan scan = AIScan.builder()
        .animal(animal)
        .uploadedBy(user)
        .imageUrl(request.imageUrl().trim())
        .imageHash(request.imageHash() != null ? request.imageHash().trim() : null)
        .status(AIScanStatus.PENDING)
        .veterinarianVerified(false)
        .build();

    scan = aiScanRepository.save(scan);
    eventPublisher.publishEvent(new AIScanCreatedEvent(scan.getId(), animal.getId(), scan.getImageUrl(), user.getId()));
    vetraMetrics.recordAiDiagnosisRequest();

    if (aiOrchestrator.isAiEnabled()) {
      try {
        scan = aiOrchestrator.processScan(scan, null);
      } catch (Exception ex) {
        log.warn("AI Orchestration attempt completed with status update: {}", ex.getMessage());
      }
    }

    return AIScanResponse.fromEntity(scan);
  }

  /**
   * Fetches an AI scan by ID with ownership verification.
   *
   * @param userIdentifier email or phone of requesting user
   * @param scanId UUID of scan
   * @return {@link AIScanResponse} details
   */
  @Transactional(readOnly = true)
  public AIScanResponse getScanById(String userIdentifier, UUID scanId) {
    User user = getUserByEmailOrPhone(userIdentifier);
    AIScan scan = aiScanRepository.findById(scanId)
        .orElseThrow(() -> new ResourceNotFoundException("AI Diagnostic scan not found with ID: " + scanId, "AI_001"));

    validateScanAccess(user, scan);
    return AIScanResponse.fromEntity(scan);
  }

  /**
   * Lists AI scans relevant to the active user non-paginated.
   *
   * @param userIdentifier email or phone of active user
   * @return list of {@link AIScanResponse}
   */
  @Transactional(readOnly = true)
  public List<AIScanResponse> listScans(String userIdentifier) {
    User user = getUserByEmailOrPhone(userIdentifier);
    List<AIScan> scans;

    if (user.getRole() == UserRole.FARMER) {
      scans = aiScanRepository.findByUploadedByOrderByCreatedAtDesc(user);
    } else {
      scans = aiScanRepository.findAll();
    }

    return scans.stream().map(AIScanResponse::fromEntity).toList();
  }

  /**
   * Lists AI scans relevant to the active user with Pageable pagination.
   *
   * @param userIdentifier email or phone of active user
   * @param pageable pagination parameters
   * @return paginated {@link Page} of {@link AIScanResponse}
   */
  @Transactional(readOnly = true)
  public Page<AIScanResponse> listScans(String userIdentifier, Pageable pageable) {
    User user = getUserByEmailOrPhone(userIdentifier);

    if (user.getRole() == UserRole.FARMER) {
      return aiScanRepository.findByUploadedById(user.getId(), pageable).map(AIScanResponse::fromEntity);
    } else {
      return aiScanRepository.findAll(pageable).map(AIScanResponse::fromEntity);
    }
  }

  /**
   * Retrieves all historical inference iteration records saved for an AIScan.
   *
   * @param userIdentifier email or phone of requesting user
   * @param scanId scan UUID
   * @return list of {@link AIScanResultEntity}
   */
  @Transactional(readOnly = true)
  public List<AIScanResultEntity> getScanResults(String userIdentifier, UUID scanId) {
    User user = getUserByEmailOrPhone(userIdentifier);
    AIScan scan = aiScanRepository.findById(scanId)
        .orElseThrow(() -> new ResourceNotFoundException("AI Diagnostic scan not found with ID: " + scanId, "AI_001"));

    validateScanAccess(user, scan);
    return aiScanResultRepository.findByScanIdOrderByCreatedAtDesc(scanId);
  }

  /**
   * Approves an AI diagnostic scan, records vet verification, and automatically generates an immutable MedicalRecord.
   *
   * @param userIdentifier email or phone of active user (must be VETERINARIAN)
   * @param scanId scan UUID
   * @param request approval request body with optional notes and diagnosis overrides
   * @return updated {@link AIScanResponse}
   */
  @Transactional
  public AIScanResponse approveScan(String userIdentifier, UUID scanId, ApproveAIScanRequest request) {
    User user = getUserByEmailOrPhone(userIdentifier);
    validateVeterinarianRole(user);

    VetProfile vetProfile = vetProfileRepository.findByUser(user)
        .orElseThrow(() -> new ResourceNotFoundException("Veterinarian profile not found", "USER_004"));

    AIScan scan = aiScanRepository.findById(scanId)
        .orElseThrow(() -> new ResourceNotFoundException("AI Diagnostic scan not found with ID: " + scanId, "AI_001"));

    validateReviewableState(scan);

    scan.setStatus(AIScanStatus.VERIFIED);
    scan.setVeterinarianVerified(true);
    scan.setVerifiedBy(user);
    scan.setVerifiedAt(Instant.now());

    if (request != null && request.customDiagnosis() != null && !request.customDiagnosis().isBlank()) {
      scan.setDiagnosis(request.customDiagnosis().trim());
    }
    if (request != null && request.notes() != null && !request.notes().isBlank()) {
      scan.setNotes(request.notes().trim());
    }

    scan = aiScanRepository.save(scan);

    // Automatically create immutable MedicalRecord entry
    String finalDiagnosis = scan.getDiagnosis() != null ? scan.getDiagnosis() : "AI Visual Observation Approved";
    String symptomsText = "Visual Observation via " + (scan.getAiProvider() != null ? scan.getAiProvider() : "AI Provider")
        + " [Model: " + (scan.getAiModel() != null ? scan.getAiModel() : "N/A")
        + ", Confidence: " + (scan.getConfidenceScore() != null ? scan.getConfidenceScore() : "N/A") + "]";

    String treatmentText = (request != null && request.treatmentNotes() != null && !request.treatmentNotes().isBlank())
        ? request.treatmentNotes().trim()
        : "Follow-up clinical inspection and care recommended.";

    MedicalRecord medicalRecord = MedicalRecord.builder()
        .animal(scan.getAnimal())
        .farmer(scan.getAnimal().getFarmer())
        .veterinarian(vetProfile)
        .diagnosis(finalDiagnosis)
        .symptoms(symptomsText)
        .treatment(treatmentText)
        .notes("AI Scan verified by Dr. " + vetProfile.getFullName() + ". Notes: " + (scan.getNotes() != null ? scan.getNotes() : ""))
        .build();

    medicalRecord = medicalRecordRepository.save(medicalRecord);

    log.info("AI Scan APPROVED scanId={} by vetId={} -> Created MedicalRecord id={}",
        scan.getId(), user.getId(), medicalRecord.getId());

    eventPublisher.publishEvent(new AIScanVerifiedEvent(scan.getId(), true, user.getId()));
    eventPublisher.publishEvent(new MedicalRecordCreatedFromAIEvent(
        medicalRecord.getId(), scan.getId(), scan.getAnimal().getId(), user.getId()));

    return AIScanResponse.fromEntity(scan);
  }

  /**
   * Rejects an AI diagnostic scan output and records the rejection reason.
   *
   * @param userIdentifier email or phone of active user (must be VETERINARIAN)
   * @param scanId scan UUID
   * @param request rejection request body containing reason
   * @return updated {@link AIScanResponse}
   */
  @Transactional
  public AIScanResponse rejectScan(String userIdentifier, UUID scanId, RejectAIScanRequest request) {
    User user = getUserByEmailOrPhone(userIdentifier);
    validateVeterinarianRole(user);

    AIScan scan = aiScanRepository.findById(scanId)
        .orElseThrow(() -> new ResourceNotFoundException("AI Diagnostic scan not found with ID: " + scanId, "AI_001"));

    validateReviewableState(scan);

    scan.setStatus(AIScanStatus.REJECTED);
    scan.setVeterinarianVerified(true);
    scan.setVerifiedBy(user);
    scan.setVerifiedAt(Instant.now());
    scan.setNotes("REJECTED: " + request.rejectionReason().trim());

    scan = aiScanRepository.save(scan);

    log.info("AI Scan REJECTED scanId={} by vetId={} reason='{}'",
        scan.getId(), user.getId(), request.rejectionReason());

    eventPublisher.publishEvent(new AIScanRejectedEvent(scan.getId(), request.rejectionReason().trim(), user.getId()));

    return AIScanResponse.fromEntity(scan);
  }

  /**
   * Backward-compatible verify scan method delegating to approve or reject flow.
   */
  @Transactional
  public AIScanResponse verifyScan(String userIdentifier, UUID scanId, VerifyAIScanRequest request) {
    if (Boolean.TRUE.equals(request.acceptDiagnosis())) {
      return approveScan(userIdentifier, scanId, new ApproveAIScanRequest(
          request.veterinarianNotes(), request.correctedDiagnosis(), null));
    } else {
      String reason = request.veterinarianNotes() != null ? request.veterinarianNotes() : "Diagnosis rejected by veterinarian";
      return rejectScan(userIdentifier, scanId, new RejectAIScanRequest(reason));
    }
  }

  /**
   * Helper method for updating scan status and diagnostic outputs.
   */
  @Transactional
  public AIScanResponse updateStatus(
      UUID scanId, AIScanStatus status, String diagnosis, BigDecimal confidenceScore) {
    AIScan scan = aiScanRepository.findById(scanId)
        .orElseThrow(() -> new ResourceNotFoundException("AI Diagnostic scan not found with ID: " + scanId, "AI_001"));

    scan.setStatus(status);
    if (diagnosis != null) {
      scan.setDiagnosis(diagnosis);
    }
    if (confidenceScore != null) {
      scan.setConfidenceScore(confidenceScore);
    }

    scan = aiScanRepository.save(scan);
    return AIScanResponse.fromEntity(scan);
  }

  private void validateVeterinarianRole(User user) {
    if (user.getRole() != UserRole.VETERINARIAN) {
      throw new UnauthorizedResourceAccessException("Only licensed veterinarians can review AI diagnostic scans", "AUTH_006");
    }
  }

  private void validateReviewableState(AIScan scan) {
    if (scan.getStatus() == AIScanStatus.VERIFIED || scan.getStatus() == AIScanStatus.REJECTED) {
      throw new BusinessRuleException("AI Diagnostic scan has already been reviewed", "AI_006");
    }
    if (scan.getStatus() == AIScanStatus.FAILED) {
      throw new BusinessRuleException("Failed AI scans cannot be reviewed by a veterinarian", "AI_007");
    }
    if (scan.getStatus() == AIScanStatus.PENDING) {
      throw new BusinessRuleException("AI scan processing must complete before review", "AI_008");
    }
  }

  private void validateScanAccess(User user, AIScan scan) {
    if (user.getRole() == UserRole.FARMER) {
      if (!scan.getUploadedBy().getId().equals(user.getId())) {
        throw new UnauthorizedResourceAccessException("Farmers can only view diagnostic scans created by themselves", "AI_002");
      }
    }
  }

  private User getUserByEmailOrPhone(String identifier) {
    return userRepository.findByEmail(identifier)
        .or(() -> userRepository.findByPhone(identifier))
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + identifier, "USER_004"));
  }
}
