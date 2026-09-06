package app.vetra.mortality.service;

import app.vetra.animal.repository.AnimalHealthRecordRepository;
import app.vetra.auth.dto.VetSummaryDto;
import app.vetra.auth.repository.VetProfileRepository;
import app.vetra.auth.service.VetDiscoveryService;
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
import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.infrastructure.persistence.enums.HealthRecordSource;
import app.vetra.infrastructure.persistence.enums.HealthRecordType;
import app.vetra.infrastructure.persistence.enums.UserRole;
import app.vetra.infrastructure.persistence.enums.VerificationStatus;
import app.vetra.mortality.dto.ConfirmMortalityRequest;
import app.vetra.mortality.dto.MortalityAuditResponse;
import app.vetra.mortality.dto.MortalityReportResponse;
import app.vetra.mortality.dto.RejectMortalityRequest;
import app.vetra.mortality.entity.AnimalMortalityEvent;
import app.vetra.mortality.entity.MortalityReferral;
import app.vetra.mortality.entity.MortalityReviewAudit;
import app.vetra.mortality.enums.MortalityCauseCategory;
import app.vetra.mortality.enums.MortalityReportStatus;
import app.vetra.mortality.enums.MortalitySource;
import app.vetra.mortality.repository.AnimalMortalityEventRepository;
import app.vetra.mortality.repository.MortalityReferralRepository;
import app.vetra.mortality.repository.MortalityReviewAuditRepository;
import app.vetra.notification.entity.NotificationChannel;
import app.vetra.notification.entity.NotificationPriority;
import app.vetra.notification.service.NotificationService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing veterinarian mortality validation, case inbox queries, audit logging,
 * and automated geographic case referrals.
 */
@Service
public class MortalityReviewService {

  private static final Logger log = LoggerFactory.getLogger(MortalityReviewService.class);

  private final AnimalMortalityEventRepository mortalityRepository;
  private final MortalityReviewAuditRepository reviewAuditRepository;
  private final MortalityReferralRepository referralRepository;
  private final VetProfileRepository vetProfileRepository;
  private final AnimalHealthRecordRepository animalHealthRecordRepository;
  private final DiseaseRegistryService diseaseRegistryService;
  private final VetDiscoveryService vetDiscoveryService;
  private final NotificationService notificationService;

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private app.vetra.disease.engine.OutbreakDetectionEngine outbreakDetectionEngine;

  /** Constructor injection for review dependencies. */
  public MortalityReviewService(
      AnimalMortalityEventRepository mortalityRepository,
      MortalityReviewAuditRepository reviewAuditRepository,
      MortalityReferralRepository referralRepository,
      VetProfileRepository vetProfileRepository,
      AnimalHealthRecordRepository animalHealthRecordRepository,
      DiseaseRegistryService diseaseRegistryService,
      VetDiscoveryService vetDiscoveryService,
      NotificationService notificationService) {
    this.mortalityRepository = mortalityRepository;
    this.reviewAuditRepository = reviewAuditRepository;
    this.referralRepository = referralRepository;
    this.vetProfileRepository = vetProfileRepository;
    this.animalHealthRecordRepository = animalHealthRecordRepository;
    this.diseaseRegistryService = diseaseRegistryService;
    this.vetDiscoveryService = vetDiscoveryService;
    this.notificationService = notificationService;
  }

  /** Setter for OutbreakDetectionEngine in testing. */
  public void setOutbreakDetectionEngine(
      app.vetra.disease.engine.OutbreakDetectionEngine outbreakDetectionEngine) {
    this.outbreakDetectionEngine = outbreakDetectionEngine;
  }

  /**
   * Refers an animal mortality event to verified nearby veterinarians based on GPS distance
   * and administrative hierarchy.
   */
  @Transactional
  public void referNearbyVeterinarians(AnimalMortalityEvent event, FarmerProfile farmer) {
    try {
      Double lat =
          event.getLatitude() != null
              ? event.getLatitude()
              : (farmer.getLatitude() != null ? farmer.getLatitude() : null);
      Double lng =
          event.getLongitude() != null
              ? event.getLongitude()
              : (farmer.getLongitude() != null ? farmer.getLongitude() : null);

      List<VetSummaryDto> nearbyVets =
          vetDiscoveryService.searchNearbyVeterinarians(
              lat, lng, 50.0, farmer.getVillage(), farmer.getTaluka(), farmer.getDistrict());

      int dispatched = 0;
      for (VetSummaryDto vetSummary : nearbyVets) {
        if (dispatched >= 5) {
          break;
        }
        if (vetSummary.id() == null) {
          continue;
        }

        Optional<VetProfile> vetOpt = vetProfileRepository.findById(vetSummary.id());
        if (vetOpt.isEmpty()) {
          continue;
        }

        VetProfile vet = vetOpt.get();
        if (vet.getVerificationStatus() != VerificationStatus.VERIFIED) {
          continue;
        }

        if (!referralRepository.existsByMortalityEventAndVeterinarian(event, vet)) {
          MortalityReferral referral =
              MortalityReferral.builder()
                  .mortalityEvent(event)
                  .veterinarian(vet)
                  .status("PENDING")
                  .distanceKm(vetSummary.distanceKm())
                  .build();
          referralRepository.save(referral);

          if (vet.getUser() != null) {
            String title = "Urgent: Mortality Case Assigned";
            String body =
                "Mortality reported for animal "
                    + event.getTagNumber()
                    + " ("
                    + event.getCauseCategory()
                    + "). Please review.";
            String payload =
                "{\"mortalityEventId\":\""
                    + event.getId()
                    + "\",\"tagNumber\":\""
                    + event.getTagNumber()
                    + "\"}";
            notificationService.sendNotification(
                vet.getUser().getId(),
                title,
                body,
                payload,
                NotificationChannel.PUSH,
                NotificationPriority.HIGH);
          }
          dispatched++;
        }
      }

      if (dispatched > 0 && event.getStatus() == MortalityReportStatus.REPORTED) {
        event.setStatus(MortalityReportStatus.PENDING_REVIEW);
        mortalityRepository.save(event);
      }
    } catch (Exception e) {
      log.warn("Failed to complete automatic mortality veterinarian referrals: {}", e.getMessage());
    }
  }

  /** Lists pending mortality validation cases referred to the authenticated veterinarian. */
  @Transactional(readOnly = true)
  public Page<MortalityReportResponse> listPendingCasesForVet(User user, Pageable pageable) {
    VetProfile vet = getVerifiedVetProfile(user);
    return referralRepository
        .findMortalityEventsByVetAndStatusIn(vet, List.of("PENDING"), pageable)
        .map(MortalityReportResponse::fromEntity);
  }

  /**
   * Confirms a mortality report by a verified veterinarian with clinical diagnosis,
   * audit logging, referral resolution, and farmer notification.
   */
  @Transactional
  @Caching(
      evict = {
        @CacheEvict(value = CacheNames.ANIMALS, allEntries = true),
        @CacheEvict(
            value = {CacheNames.DASHBOARD_FARMER, CacheNames.DASHBOARD_VET, CacheNames.ANALYTICS},
            allEntries = true)
      })
  public MortalityReportResponse confirmMortality(
      User user, UUID eventId, ConfirmMortalityRequest request) {

    VetProfile vet = getVerifiedVetProfile(user);
    AnimalMortalityEvent event =
        mortalityRepository
            .findById(eventId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Mortality report not found with ID: " + eventId, "MORTALITY_002"));

    if (event.getStatus() == MortalityReportStatus.CONFIRMED) {
      if (event.getVetReviewedBy() != null && event.getVetReviewedBy().getId().equals(vet.getId())) {
        return MortalityReportResponse.fromEntity(event);
      }
      throw new ConflictException("Mortality report has already been confirmed", "MORTALITY_003");
    }

    if (event.getStatus() == MortalityReportStatus.REJECTED) {
      throw new ConflictException("Mortality report has already been rejected", "MORTALITY_003");
    }

    String resolvedDisease = resolveDiseaseName(request.causeCategory(), request.diseaseName());
    MortalityReportStatus oldStatus = event.getStatus();

    event.setStatus(MortalityReportStatus.CONFIRMED);
    event.setSource(MortalitySource.VET_CONFIRMED);
    event.setVetReviewedBy(vet);
    event.setVetReviewedAt(Instant.now());
    event.setVetCauseCategory(request.causeCategory());
    event.setVetDiseaseName(resolvedDisease);
    event.setVetClinicalNotes(request.clinicalNotes());
    event.setPostMortemConducted(Boolean.TRUE.equals(request.postMortemConducted()));
    event = mortalityRepository.save(event);

    MortalityReviewAudit audit =
        MortalityReviewAudit.builder()
            .mortalityEvent(event)
            .veterinarian(vet)
            .action("CONFIRM")
            .oldStatus(oldStatus)
            .newStatus(MortalityReportStatus.CONFIRMED)
            .clinicalNotes(request.clinicalNotes())
            .confirmedCauseCategory(request.causeCategory())
            .confirmedDiseaseName(resolvedDisease)
            .build();
    reviewAuditRepository.save(audit);

    recordHealthRecordAudit(
        event.getAnimal(),
        "Mortality Confirmed: " + request.causeCategory(),
        "Verified by Dr. " + vet.getFullName() + ". Notes: " + request.clinicalNotes(),
        resolvedDisease);

    resolveReferrals(event, vet);
    notifyFarmer(event, vet, "Mortality Case Verified", "confirmed mortality for");

    if (outbreakDetectionEngine != null) {
      try {
        outbreakDetectionEngine.evaluateMortalityEvent(event);
      } catch (Exception e) {
        log.warn("Failed to evaluate outbreak risk on mortality confirmation: {}", e.getMessage());
      }
    }

    return MortalityReportResponse.fromEntity(event);
  }

  /**
   * Rejects a mortality report with documented clinical rationale.
   */
  @Transactional
  @Caching(
      evict = {
        @CacheEvict(value = CacheNames.ANIMALS, allEntries = true),
        @CacheEvict(
            value = {CacheNames.DASHBOARD_FARMER, CacheNames.DASHBOARD_VET, CacheNames.ANALYTICS},
            allEntries = true)
      })
  public MortalityReportResponse rejectMortality(
      User user, UUID eventId, RejectMortalityRequest request) {

    VetProfile vet = getVerifiedVetProfile(user);
    AnimalMortalityEvent event =
        mortalityRepository
            .findById(eventId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Mortality report not found with ID: " + eventId, "MORTALITY_002"));

    if (event.getStatus() == MortalityReportStatus.CONFIRMED
        || event.getStatus() == MortalityReportStatus.REJECTED) {
      throw new ConflictException("Mortality report has already been reviewed", "MORTALITY_003");
    }

    MortalityReportStatus oldStatus = event.getStatus();

    event.setStatus(MortalityReportStatus.REJECTED);
    event.setVetReviewedBy(vet);
    event.setVetReviewedAt(Instant.now());
    event.setVetRejectionReason(request.rejectionReason());
    event.setVetClinicalNotes(request.clinicalNotes());
    event = mortalityRepository.save(event);

    MortalityReviewAudit audit =
        MortalityReviewAudit.builder()
            .mortalityEvent(event)
            .veterinarian(vet)
            .action("REJECT")
            .oldStatus(oldStatus)
            .newStatus(MortalityReportStatus.REJECTED)
            .clinicalNotes("Reason: " + request.rejectionReason() + ". " + request.clinicalNotes())
            .build();
    reviewAuditRepository.save(audit);

    recordHealthRecordAudit(
        event.getAnimal(),
        "Mortality Report Rejected",
        "Rejected by Dr. " + vet.getFullName() + ": " + request.rejectionReason(),
        null);

    resolveReferrals(event, vet);
    notifyFarmer(event, vet, "Mortality Report Rejected", "rejected the mortality report for");

    return MortalityReportResponse.fromEntity(event);
  }

  /** Retrieves full audit trail for a mortality report. */
  @Transactional(readOnly = true)
  public List<MortalityAuditResponse> getAuditsForEvent(UUID eventId) {
    return reviewAuditRepository.findByMortalityEventIdOrderByCreatedAtAsc(eventId).stream()
        .map(MortalityAuditResponse::fromEntity)
        .toList();
  }

  private VetProfile getVerifiedVetProfile(User user) {
    if (user.getRole() != UserRole.VETERINARIAN) {
      throw new UnauthorizedResourceAccessException(
          "Access denied: Only veterinarians can perform mortality validations", "AUTH_006");
    }
    VetProfile vet =
        vetProfileRepository
            .findByUser(user)
            .orElseThrow(
                () -> new ResourceNotFoundException("Veterinarian profile not found", "USER_004"));
    if (vet.getVerificationStatus() != VerificationStatus.VERIFIED) {
      throw new UnauthorizedResourceAccessException(
          "Access denied: Only verified veterinarians can validate mortality cases", "AUTH_003");
    }
    return vet;
  }

  private String resolveDiseaseName(MortalityCauseCategory causeCategory, String diseaseName) {
    if (causeCategory == MortalityCauseCategory.UNKNOWN) {
      return null;
    }
    if (diseaseName != null && !diseaseName.isBlank()) {
      String trimmed = diseaseName.trim();
      Optional<DiseaseMetadata> metadata = diseaseRegistryService.getDiseaseByName(trimmed);
      return metadata.map(DiseaseMetadata::diseaseName).orElse(trimmed);
    }
    return null;
  }

  private void recordHealthRecordAudit(
      Animal animal, String title, String description, String diagnosis) {
    try {
      AnimalHealthRecord record =
          AnimalHealthRecord.builder()
              .animal(animal)
              .recordType(HealthRecordType.MORTALITY)
              .source(HealthRecordSource.VETERINARIAN)
              .title(title)
              .description(description)
              .diagnosis(diagnosis)
              .recordedAt(LocalDateTime.now(ZoneOffset.UTC))
              .build();
      animalHealthRecordRepository.save(record);
    } catch (Exception e) {
      log.warn("Failed to write health record audit for mortality review: {}", e.getMessage());
    }
  }

  private void resolveReferrals(AnimalMortalityEvent event, VetProfile vet) {
    try {
      List<MortalityReferral> referrals = referralRepository.findByMortalityEventId(event.getId());
      for (MortalityReferral ref : referrals) {
        if (ref.getVeterinarian().getId().equals(vet.getId())) {
          ref.setStatus("RESOLVED");
        } else if ("PENDING".equals(ref.getStatus())) {
          ref.setStatus("CLOSED");
        }
      }
      referralRepository.saveAll(referrals);
    } catch (Exception e) {
      log.warn("Failed to resolve mortality referrals: {}", e.getMessage());
    }
  }

  private void notifyFarmer(
      AnimalMortalityEvent event, VetProfile vet, String title, String actionDescription) {
    try {
      if (event.getFarmer() != null && event.getFarmer().getUser() != null) {
        User farmerUser = event.getFarmer().getUser();
        String body =
            "Dr. "
                + vet.getFullName()
                + " "
                + actionDescription
                + " animal "
                + event.getTagNumber()
                + ".";
        String payload =
            "{\"mortalityEventId\":\""
                + event.getId()
                + "\",\"status\":\""
                + event.getStatus()
                + "\"}";
        notificationService.sendNotification(
            farmerUser.getId(),
            title,
            body,
            payload,
            NotificationChannel.PUSH,
            NotificationPriority.NORMAL);
      }
    } catch (Exception e) {
      log.warn("Failed to notify farmer about mortality validation: {}", e.getMessage());
    }
  }
}
