package app.vetra.mortality.service;

import app.vetra.animal.repository.AnimalHealthRecordRepository;
import app.vetra.animal.repository.AnimalRepository;
import app.vetra.auth.repository.FarmerProfileRepository;
import app.vetra.auth.repository.UserRepository;
import app.vetra.disease.registry.DiseaseMetadata;
import app.vetra.disease.registry.DiseaseRegistryService;
import app.vetra.infrastructure.cache.CacheNames;
import app.vetra.infrastructure.exception.ConflictException;
import app.vetra.infrastructure.exception.ResourceNotFoundException;
import app.vetra.infrastructure.exception.UnauthorizedResourceAccessException;
import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.entity.AnimalHealthRecord;
import app.vetra.infrastructure.persistence.entity.FarmerProfile;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.enums.AnimalStatus;
import app.vetra.infrastructure.persistence.enums.HealthRecordSource;
import app.vetra.infrastructure.persistence.enums.HealthRecordType;
import app.vetra.infrastructure.persistence.enums.UserRole;
import app.vetra.mortality.dto.ConfirmMortalityRequest;
import app.vetra.mortality.dto.CreateMortalityReportRequest;
import app.vetra.mortality.dto.MortalityAuditResponse;
import app.vetra.mortality.dto.MortalityReportResponse;
import app.vetra.mortality.dto.RejectMortalityRequest;
import app.vetra.mortality.entity.AnimalMortalityEvent;
import app.vetra.mortality.enums.MortalityCauseCategory;
import app.vetra.mortality.enums.MortalityReportStatus;
import app.vetra.mortality.enums.MortalitySource;
import app.vetra.mortality.repository.AnimalMortalityEventRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business service for animal mortality reporting and lifecycle management. */
@Service
public class MortalityService {

  private final AnimalMortalityEventRepository mortalityRepository;
  private final AnimalRepository animalRepository;
  private final UserRepository userRepository;
  private final FarmerProfileRepository farmerProfileRepository;
  private final AnimalHealthRecordRepository animalHealthRecordRepository;
  private final DiseaseRegistryService diseaseRegistryService;
  private final MortalityReviewService mortalityReviewService;

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private app.vetra.disease.engine.OutbreakDetectionEngine outbreakDetectionEngine;

  /** Constructor injection. */
  public MortalityService(
      AnimalMortalityEventRepository mortalityRepository,
      AnimalRepository animalRepository,
      UserRepository userRepository,
      FarmerProfileRepository farmerProfileRepository,
      AnimalHealthRecordRepository animalHealthRecordRepository,
      DiseaseRegistryService diseaseRegistryService,
      MortalityReviewService mortalityReviewService) {
    this.mortalityRepository = mortalityRepository;
    this.animalRepository = animalRepository;
    this.userRepository = userRepository;
    this.farmerProfileRepository = farmerProfileRepository;
    this.animalHealthRecordRepository = animalHealthRecordRepository;
    this.diseaseRegistryService = diseaseRegistryService;
    this.mortalityReviewService = mortalityReviewService;
  }

  /** Setter for OutbreakDetectionEngine in unit testing. */
  public void setOutbreakDetectionEngine(
      app.vetra.disease.engine.OutbreakDetectionEngine outbreakDetectionEngine) {
    this.outbreakDetectionEngine = outbreakDetectionEngine;
  }

  /**
   * Records a new animal mortality event reported by the animal's owner farmer.
   *
   * <p>Enforces strict role authorization, verified animal ownership, prevents duplicate
   * deceased submissions, sets animal status to DECEASED, creates an audit health record,
   * and triggers geographic referral to nearby verified veterinarians.
   */
  @Transactional
  @Caching(
      evict = {
        @CacheEvict(value = CacheNames.ANIMALS, allEntries = true),
        @CacheEvict(
            value = {CacheNames.DASHBOARD_FARMER, CacheNames.ANALYTICS},
            allEntries = true)
      })
  public MortalityReportResponse createMortalityReport(
      String currentUserIdentifier, CreateMortalityReportRequest request) {

    User user = getUserByHeader(currentUserIdentifier);
    if (user.getRole() != UserRole.FARMER) {
      throw new UnauthorizedResourceAccessException(
          "Only farmers can submit animal mortality reports", "AUTH_006");
    }

    FarmerProfile farmer =
        farmerProfileRepository
            .findByUser(user)
            .orElseThrow(
                () -> new ResourceNotFoundException("Farmer profile not found", "USER_004"));

    Animal animal =
        animalRepository
            .findById(request.animalId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Animal not found with ID: " + request.animalId(), "ANIMAL_001"));

    if (!animal.getFarmer().getId().equals(farmer.getId())) {
      throw new UnauthorizedResourceAccessException(
          "Access denied: You do not own this animal record", "ANIMAL_002");
    }

    if (animal.getStatus() == AnimalStatus.DECEASED
        || mortalityRepository.existsByAnimal(animal)) {
      throw new ConflictException(
          "Animal is already recorded as deceased: " + animal.getTagNumber(), "MORTALITY_001");
    }

    String resolvedDiseaseName = resolveDiseaseName(request);

    AnimalMortalityEvent event =
        AnimalMortalityEvent.builder()
            .animal(animal)
            .farmer(farmer)
            .reportedBy(user)
            .tagNumber(animal.getTagNumber())
            .qrCodeId(animal.getQrCodeId())
            .causeCategory(request.causeCategory())
            .causeDescription(request.causeDescription())
            .diseaseName(resolvedDiseaseName)
            .recentlyTreated(Boolean.TRUE.equals(request.recentlyTreated()))
            .treatmentNotes(request.treatmentNotes())
            .notes(request.notes())
            .source(MortalitySource.FARMER_REPORTED)
            .status(MortalityReportStatus.REPORTED)
            .deathDateTime(request.deathDateTime() != null ? request.deathDateTime() : Instant.now())
            .reportedAt(Instant.now())
            .latitude(request.latitude())
            .longitude(request.longitude())
            .locationAccuracy(request.locationAccuracy())
            .build();

    event = mortalityRepository.save(event);

    animal.setStatus(AnimalStatus.DECEASED);
    animalRepository.save(animal);

    createHealthRecordEntry(animal, event);

    mortalityReviewService.referNearbyVeterinarians(event, farmer);

    if (outbreakDetectionEngine != null && event.getDiseaseName() != null) {
      try {
        outbreakDetectionEngine.evaluateMortalityEvent(event);
      } catch (Exception e) {
        // Non-fatal epidemiological evaluation failure
      }
    }

    return MortalityReportResponse.fromEntity(event);
  }

  /** Retrieves a mortality report by its UUID with ownership verification. */
  @Transactional(readOnly = true)
  public MortalityReportResponse getMortalityById(String currentUserIdentifier, UUID id) {
    User user = getUserByHeader(currentUserIdentifier);
    AnimalMortalityEvent event =
        mortalityRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Mortality report not found with ID: " + id, "MORTALITY_002"));

    if (user.getRole() == UserRole.FARMER
        && !event.getFarmer().getUser().getId().equals(user.getId())) {
      throw new UnauthorizedResourceAccessException(
          "Access denied: You do not have permission to view this report", "AUTH_003");
    }

    return MortalityReportResponse.fromEntity(event);
  }

  /** Retrieves the mortality report for a specific animal. */
  @Transactional(readOnly = true)
  public MortalityReportResponse getMortalityByAnimalId(
      String currentUserIdentifier, UUID animalId) {
    User user = getUserByHeader(currentUserIdentifier);
    AnimalMortalityEvent event =
        mortalityRepository
            .findByAnimalId(animalId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "No mortality event recorded for animal: " + animalId, "MORTALITY_002"));

    if (user.getRole() == UserRole.FARMER
        && !event.getFarmer().getUser().getId().equals(user.getId())) {
      throw new UnauthorizedResourceAccessException(
          "Access denied: You do not have permission to view this report", "AUTH_003");
    }

    return MortalityReportResponse.fromEntity(event);
  }

  /** Lists mortality reports for the authenticated farmer with pagination. */
  @Transactional(readOnly = true)
  public Page<MortalityReportResponse> listFarmerMortalities(
      String currentUserIdentifier, Pageable pageable) {
    User user = getUserByHeader(currentUserIdentifier);
    FarmerProfile farmer =
        farmerProfileRepository
            .findByUser(user)
            .orElseThrow(
                () -> new ResourceNotFoundException("Farmer profile not found", "USER_004"));

    return mortalityRepository
        .findByFarmer(farmer, pageable)
        .map(MortalityReportResponse::fromEntity);
  }

  /** Lists pending mortality validation cases referred to the authenticated veterinarian. */
  @Transactional(readOnly = true)
  public Page<MortalityReportResponse> listPendingCasesForVet(
      String currentUserIdentifier, Pageable pageable) {
    User user = getUserByHeader(currentUserIdentifier);
    return mortalityReviewService.listPendingCasesForVet(user, pageable);
  }

  /** Confirms a mortality report by a verified veterinarian. */
  @Transactional
  public MortalityReportResponse confirmMortality(
      String currentUserIdentifier, UUID id, ConfirmMortalityRequest request) {
    User user = getUserByHeader(currentUserIdentifier);
    return mortalityReviewService.confirmMortality(user, id, request);
  }

  /** Rejects a mortality report by a verified veterinarian. */
  @Transactional
  public MortalityReportResponse rejectMortality(
      String currentUserIdentifier, UUID id, RejectMortalityRequest request) {
    User user = getUserByHeader(currentUserIdentifier);
    return mortalityReviewService.rejectMortality(user, id, request);
  }

  /** Retrieves full audit history for a mortality report. */
  @Transactional(readOnly = true)
  public List<MortalityAuditResponse> getAuditsForEvent(
      String currentUserIdentifier, UUID id) {
    User user = getUserByHeader(currentUserIdentifier);
    // Verify user has access to this event
    AnimalMortalityEvent event =
        mortalityRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Mortality report not found with ID: " + id, "MORTALITY_002"));

    if (user.getRole() == UserRole.FARMER
        && !event.getFarmer().getUser().getId().equals(user.getId())) {
      throw new UnauthorizedResourceAccessException(
          "Access denied: You do not have permission to view audits for this report", "AUTH_003");
    }

    return mortalityReviewService.getAuditsForEvent(id);
  }

  private String resolveDiseaseName(CreateMortalityReportRequest request) {
    if (request.causeCategory() == MortalityCauseCategory.UNKNOWN) {
      return null;
    }
    if (request.diseaseName() != null && !request.diseaseName().isBlank()) {
      String trimmed = request.diseaseName().trim();
      Optional<DiseaseMetadata> metadata = diseaseRegistryService.getDiseaseByName(trimmed);
      return metadata.map(DiseaseMetadata::diseaseName).orElse(trimmed);
    }
    return null;
  }

  private void createHealthRecordEntry(Animal animal, AnimalMortalityEvent event) {
    try {
      LocalDateTime recordedTime =
          event.getDeathDateTime() != null
              ? LocalDateTime.ofInstant(event.getDeathDateTime(), ZoneOffset.UTC)
              : LocalDateTime.now();

      String title = "Mortality Reported: " + event.getCauseCategory();
      String desc =
          event.getCauseDescription() != null
              ? event.getCauseDescription()
              : "Animal reported deceased by farmer (" + event.getCauseCategory() + ").";

      AnimalHealthRecord record =
          AnimalHealthRecord.builder()
              .animal(animal)
              .recordType(HealthRecordType.MORTALITY)
              .source(HealthRecordSource.FARMER)
              .title(title)
              .description(desc)
              .diagnosis(event.getDiseaseName())
              .treatment(event.getTreatmentNotes())
              .recordedAt(recordedTime)
              .build();

      animalHealthRecordRepository.save(record);
    } catch (Exception e) {
      // Non-fatal audit log failure: main mortality event is already committed.
    }
  }

  private User getUserByHeader(String identifier) {
    return userRepository
        .findByIdentifier(identifier)
        .orElseThrow(
            () -> new ResourceNotFoundException("User not found: " + identifier, "USER_004"));
  }
}
