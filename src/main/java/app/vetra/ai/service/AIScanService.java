package app.vetra.ai.service;

import app.vetra.ai.config.AIProperties;
import app.vetra.ai.dto.AIScanResponse;
import app.vetra.ai.dto.CreateAIScanRequest;
import app.vetra.ai.dto.VerifyAIScanRequest;
import app.vetra.ai.entity.AIScan;
import app.vetra.ai.entity.AIScanResultEntity;
import app.vetra.ai.entity.AIScanStatus;
import app.vetra.ai.event.AIScanCreatedEvent;
import app.vetra.ai.event.AIScanVerifiedEvent;
import app.vetra.ai.orchestrator.AIOrchestrator;
import app.vetra.ai.repository.AIScanRepository;
import app.vetra.ai.repository.AIScanResultRepository;
import app.vetra.animal.repository.AnimalRepository;
import app.vetra.auth.repository.FarmerProfileRepository;
import app.vetra.auth.repository.UserRepository;
import app.vetra.infrastructure.exception.ResourceNotFoundException;
import app.vetra.infrastructure.exception.UnauthorizedResourceAccessException;
import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.entity.FarmerProfile;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.enums.UserRole;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core business service managing AI diagnostic scan requests, status updates, and veterinarian verification.
 */
@Service
public class AIScanService {

  private static final Logger log = LoggerFactory.getLogger(AIScanService.class);

  private final AIScanRepository aiScanRepository;
  private final AIScanResultRepository aiScanResultRepository;
  private final AnimalRepository animalRepository;
  private final UserRepository userRepository;
  private final FarmerProfileRepository farmerProfileRepository;
  private final AIOrchestrator aiOrchestrator;
  private final AIProperties aiProperties;
  private final ApplicationEventPublisher eventPublisher;

  /** Constructor injection. */
  public AIScanService(
      AIScanRepository aiScanRepository,
      AIScanResultRepository aiScanResultRepository,
      AnimalRepository animalRepository,
      UserRepository userRepository,
      FarmerProfileRepository farmerProfileRepository,
      AIOrchestrator aiOrchestrator,
      AIProperties aiProperties,
      ApplicationEventPublisher eventPublisher) {
    this.aiScanRepository = aiScanRepository;
    this.aiScanResultRepository = aiScanResultRepository;
    this.animalRepository = animalRepository;
    this.userRepository = userRepository;
    this.farmerProfileRepository = farmerProfileRepository;
    this.aiOrchestrator = aiOrchestrator;
    this.aiProperties = aiProperties;
    this.eventPublisher = eventPublisher;
  }

  /**
   * Registers a new AI diagnostic scan request for an animal and triggers orchestration if enabled.
   *
   * @param userIdentifier email or phone of authenticated requesting user
   * @param request create scan request parameters
   * @return {@link AIScanResponse} created record
   */
  @Transactional
  public AIScanResponse createScan(String userIdentifier, CreateAIScanRequest request) {
    User user = getUserByEmailOrPhone(userIdentifier);
    Animal animal = animalRepository.findById(request.animalId())
        .orElseThrow(() -> new ResourceNotFoundException("Animal not found with ID: " + request.animalId(), "ANIMAL_001"));

    if (user.getRole() == UserRole.FARMER) {
      FarmerProfile farmer = farmerProfileRepository.findByUser(user)
          .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found", "USER_004"));
      if (!animal.getFarmer().getId().equals(farmer.getId())) {
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

    if (aiProperties.isEnabled()) {
      try {
        scan = aiOrchestrator.processScan(scan, aiProperties.getDefaultProvider());
      } catch (Exception ex) {
        log.warn("AI Orchestration attempt completed with error: {}", ex.getMessage());
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
   * Allows a licensed veterinarian to verify or correct an AI diagnostic scan.
   *
   * @param userIdentifier email or phone of active user (must be VETERINARIAN)
   * @param scanId scan UUID
   * @param request verification request body
   * @return updated {@link AIScanResponse}
   */
  @Transactional
  public AIScanResponse verifyScan(String userIdentifier, UUID scanId, VerifyAIScanRequest request) {
    User user = getUserByEmailOrPhone(userIdentifier);

    if (user.getRole() != UserRole.VETERINARIAN) {
      throw new UnauthorizedResourceAccessException("Only licensed veterinarians can verify AI diagnostic scans", "AUTH_006");
    }

    AIScan scan = aiScanRepository.findById(scanId)
        .orElseThrow(() -> new ResourceNotFoundException("AI Diagnostic scan not found with ID: " + scanId, "AI_001"));

    scan.setVeterinarianVerified(true);
    scan.setVerifiedBy(user);
    scan.setVerifiedAt(Instant.now());

    boolean accepted = Boolean.TRUE.equals(request.acceptDiagnosis());
    if (accepted) {
      scan.setStatus(AIScanStatus.VERIFIED);
    } else {
      scan.setStatus(AIScanStatus.REJECTED);
      if (request.correctedDiagnosis() != null && !request.correctedDiagnosis().isBlank()) {
        scan.setDiagnosis(request.correctedDiagnosis().trim());
      }
    }

    if (request.veterinarianNotes() != null && !request.veterinarianNotes().isBlank()) {
      scan.setNotes(request.veterinarianNotes().trim());
    }

    scan = aiScanRepository.save(scan);
    eventPublisher.publishEvent(new AIScanVerifiedEvent(scan.getId(), accepted, user.getId()));
    return AIScanResponse.fromEntity(scan);
  }

  /**
   * Helper method for updating scan status and diagnostic outputs.
   *
   * @param scanId scan UUID
   * @param status target status
   * @param diagnosis diagnostic text output
   * @param confidenceScore confidence score
   * @return updated {@link AIScanResponse}
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
