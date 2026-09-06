package app.vetra.vaccination.service;

import app.vetra.animal.repository.AnimalHealthRecordRepository;
import app.vetra.animal.repository.AnimalRepository;
import app.vetra.disease.entity.Outbreak;
import app.vetra.disease.geo.AdministrativeBoundaryService;
import app.vetra.disease.registry.DiseaseRegistryService;
import app.vetra.disease.repository.OutbreakRepository;
import app.vetra.infrastructure.cache.CacheNames;
import app.vetra.infrastructure.exception.ResourceNotFoundException;
import app.vetra.infrastructure.exception.UnauthorizedResourceAccessException;
import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.entity.AnimalHealthRecord;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.enums.AnimalStatus;
import app.vetra.infrastructure.persistence.enums.HealthRecordType;
import app.vetra.infrastructure.persistence.enums.UserRole;
import app.vetra.auth.repository.UserRepository;
import app.vetra.vaccination.dto.CreateVaccinationCampaignRequest;
import app.vetra.vaccination.dto.UpdateCampaignStatusRequest;
import app.vetra.vaccination.dto.VaccinationCampaignAuditLogResponse;
import app.vetra.vaccination.dto.VaccinationCampaignResponse;
import app.vetra.vaccination.dto.VaccinationCampaignStatisticsResponse;
import app.vetra.vaccination.entity.VaccinationCampaign;
import app.vetra.vaccination.entity.VaccinationCampaignAuditLog;
import app.vetra.vaccination.enums.CampaignPriority;
import app.vetra.vaccination.enums.CampaignStatus;
import app.vetra.vaccination.repository.VaccinationCampaignAuditLogRepository;
import app.vetra.vaccination.repository.VaccinationCampaignRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core business service managing official government vaccination campaigns.
 */
@Service
public class VaccinationCampaignService {

  private final VaccinationCampaignRepository campaignRepository;
  private final VaccinationCampaignAuditLogRepository auditLogRepository;
  private final UserRepository userRepository;
  private final DiseaseRegistryService diseaseRegistryService;
  private final AdministrativeBoundaryService boundaryService;
  private final OutbreakRepository outbreakRepository;
  private final AnimalRepository animalRepository;
  private final AnimalHealthRecordRepository healthRecordRepository;

  /**
   * Constructor injection for campaign management service.
   */
  public VaccinationCampaignService(
      VaccinationCampaignRepository campaignRepository,
      VaccinationCampaignAuditLogRepository auditLogRepository,
      UserRepository userRepository,
      DiseaseRegistryService diseaseRegistryService,
      AdministrativeBoundaryService boundaryService,
      OutbreakRepository outbreakRepository,
      AnimalRepository animalRepository,
      AnimalHealthRecordRepository healthRecordRepository) {
    this.campaignRepository = campaignRepository;
    this.auditLogRepository = auditLogRepository;
    this.userRepository = userRepository;
    this.diseaseRegistryService = diseaseRegistryService;
    this.boundaryService = boundaryService;
    this.outbreakRepository = outbreakRepository;
    this.animalRepository = animalRepository;
    this.healthRecordRepository = healthRecordRepository;
  }

  /**
   * Creates and launches a new vaccination campaign.
   *
   * @param officerIdentifier authentication principal identifier
   * @param request campaign launch request
   * @return created {@link VaccinationCampaignResponse}
   */
  @Transactional
  @Caching(
      evict = {
        @CacheEvict(value = CacheNames.ANALYTICS, allEntries = true),
        @CacheEvict(value = CacheNames.DASHBOARD_ADMIN, allEntries = true)
      })
  public VaccinationCampaignResponse createCampaign(
      String officerIdentifier, CreateVaccinationCampaignRequest request) {

    User officer = getAuthenticatedOfficer(officerIdentifier);

    // Validate disease against disease taxonomy registry
    if (diseaseRegistryService.getDiseaseByName(request.diseaseName()).isEmpty()) {
      throw new IllegalArgumentException("Unknown disease: " + request.diseaseName());
    }

    // Validate target district against boundary service
    validateDistrict(request.targetDistrict());

    // Validate planned doses
    if (request.plannedDoses() == null || request.plannedDoses() <= 0) {
      throw new IllegalArgumentException("Planned doses must be greater than zero");
    }

    // Validate optional linked outbreak
    Outbreak outbreak = null;
    if (request.outbreakId() != null) {
      outbreak = outbreakRepository.findById(request.outbreakId())
          .orElseThrow(() -> new ResourceNotFoundException(
              "Linked outbreak not found: " + request.outbreakId(), "OUTBREAK_001"));
    }

    // Resolve target livestock count using susceptible definition if not explicitly provided
    int targetLivestock = request.targetLivestockCount() != null && request.targetLivestockCount() > 0
        ? request.targetLivestockCount()
        : calculateSusceptibleLivestock(request.diseaseName(), request.targetDistrict(), request.targetTaluka());

    CampaignPriority priority = request.priority() != null ? request.priority() : CampaignPriority.MEDIUM;

    VaccinationCampaign campaign = VaccinationCampaign.builder()
        .campaignName(request.campaignName().trim())
        .diseaseName(request.diseaseName().trim())
        .targetDistrict(request.targetDistrict().trim())
        .targetTaluka(request.targetTaluka() != null ? request.targetTaluka().trim() : null)
        .targetLivestockCount(targetLivestock)
        .plannedDoses(request.plannedDoses())
        .administeredDoses(0)
        .priority(priority)
        .status(CampaignStatus.PLANNED)
        .startDate(request.startDate())
        .endDate(request.endDate())
        .outbreak(outbreak)
        .createdBy(officer)
        .notes(request.notes())
        .build();

    VaccinationCampaign saved = campaignRepository.save(campaign);

    // Record initial creation in immutable audit trail
    VaccinationCampaignAuditLog auditLog = VaccinationCampaignAuditLog.builder()
        .campaign(saved)
        .performedBy(officer)
        .action("CREATED")
        .previousStatus(null)
        .newStatus(CampaignStatus.PLANNED.name())
        .notes(request.notes())
        .build();
    auditLogRepository.save(auditLog);

    return VaccinationCampaignResponse.fromEntity(saved);
  }

  /**
   * Retrieves a single campaign by ID.
   *
   * @param id campaign UUID
   * @return {@link VaccinationCampaignResponse}
   */
  @Transactional(readOnly = true)
  public VaccinationCampaignResponse getCampaignById(UUID id) {
    VaccinationCampaign campaign = campaignRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + id, "CAMPAIGN_001"));
    return VaccinationCampaignResponse.fromEntity(campaign);
  }

  /**
   * Lists campaigns with optional status and district filtering.
   *
   * @param status optional status filter
   * @param district optional district filter
   * @param pageable pagination parameters
   * @return paginated campaign responses
   */
  @Transactional(readOnly = true)
  public Page<VaccinationCampaignResponse> listCampaigns(
      CampaignStatus status, String district, Pageable pageable) {

    Page<VaccinationCampaign> page;
    boolean hasDistrict = district != null && !district.isBlank() && !"ALL".equalsIgnoreCase(district.trim());

    if (status != null && hasDistrict) {
      page = campaignRepository.findByStatusAndTargetDistrictIgnoreCase(status, district.trim(), pageable);
    } else if (status != null) {
      page = campaignRepository.findByStatus(status, pageable);
    } else if (hasDistrict) {
      page = campaignRepository.findByTargetDistrictIgnoreCase(district.trim(), pageable);
    } else {
      page = campaignRepository.findAll(pageable);
    }

    return page.map(VaccinationCampaignResponse::fromEntity);
  }

  /**
   * Retrieves all active vaccination campaigns.
   *
   * @return list of active campaign responses
   */
  @Transactional(readOnly = true)
  public List<VaccinationCampaignResponse> getActiveCampaigns() {
    return campaignRepository.findByStatus(CampaignStatus.ACTIVE).stream()
        .map(VaccinationCampaignResponse::fromEntity)
        .toList();
  }

  /**
   * Updates the lifecycle status of an existing campaign.
   *
   * @param officerIdentifier authentication principal identifier
   * @param id campaign UUID
   * @param request status transition request
   * @return updated {@link VaccinationCampaignResponse}
   */
  @Transactional
  @Caching(
      evict = {
        @CacheEvict(value = CacheNames.ANALYTICS, allEntries = true),
        @CacheEvict(value = CacheNames.DASHBOARD_ADMIN, allEntries = true)
      })
  public VaccinationCampaignResponse updateCampaignStatus(
      String officerIdentifier, UUID id, UpdateCampaignStatusRequest request) {

    User officer = getAuthenticatedOfficer(officerIdentifier);

    VaccinationCampaign campaign = campaignRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + id, "CAMPAIGN_001"));

    CampaignStatus oldStatus = campaign.getStatus();
    campaign.transitionTo(request.status());

    if (request.notes() != null && !request.notes().isBlank()) {
      String existingNotes = campaign.getNotes() != null ? campaign.getNotes() + "\n" : "";
      campaign.setNotes(existingNotes + "[" + request.status() + "] " + request.notes());
    }

    VaccinationCampaign updated = campaignRepository.save(campaign);

    // Record status transition in audit log
    VaccinationCampaignAuditLog auditLog = VaccinationCampaignAuditLog.builder()
        .campaign(updated)
        .performedBy(officer)
        .action("STATUS_CHANGED")
        .previousStatus(oldStatus.name())
        .newStatus(request.status().name())
        .notes(request.notes())
        .build();
    auditLogRepository.save(auditLog);

    return VaccinationCampaignResponse.fromEntity(updated);
  }

  /**
   * Retrieves the immutable audit log trail for a campaign.
   *
   * @param campaignId campaign UUID
   * @return list of audit log records
   */
  @Transactional(readOnly = true)
  public List<VaccinationCampaignAuditLogResponse> getCampaignAuditLogs(UUID campaignId) {
    if (!campaignRepository.existsById(campaignId)) {
      throw new ResourceNotFoundException("Campaign not found: " + campaignId, "CAMPAIGN_001");
    }
    return auditLogRepository.findByCampaignIdOrderByCreatedAtDesc(campaignId).stream()
        .map(VaccinationCampaignAuditLogResponse::fromEntity)
        .toList();
  }

  /**
   * Computes regional campaign summary statistics.
   *
   * @return {@link VaccinationCampaignStatisticsResponse}
   */
  @Transactional(readOnly = true)
  public VaccinationCampaignStatisticsResponse getCampaignStatistics() {
    long total = campaignRepository.count();
    long planned = campaignRepository.countByStatus(CampaignStatus.PLANNED);
    long active = campaignRepository.countByStatus(CampaignStatus.ACTIVE);
    long completed = campaignRepository.countByStatus(CampaignStatus.COMPLETED);
    long cancelled = campaignRepository.countByStatus(CampaignStatus.CANCELLED);
    long totalPlanned = campaignRepository.sumPlannedDoses();
    long totalAdministered = campaignRepository.sumAdministeredDoses();

    double progress = totalPlanned > 0
        ? Math.min(100.0, ((double) totalAdministered / totalPlanned) * 100.0)
        : 0.0;

    return new VaccinationCampaignStatisticsResponse(
        total, planned, active, completed, cancelled, totalPlanned, totalAdministered, progress);
  }

  private User getAuthenticatedOfficer(String identifier) {
    User user = userRepository.findByIdentifier(identifier)
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + identifier, "USER_004"));

    if (user.getRole() != UserRole.GOVERNMENT_OFFICER && user.getRole() != UserRole.ADMINISTRATOR) {
      throw new UnauthorizedResourceAccessException(
          "Only government officers and administrators can manage vaccination campaigns", "AUTH_006");
    }
    return user;
  }

  private void validateDistrict(String district) {
    List<String> availableDistricts = boundaryService.getAvailableDistricts();
    if (availableDistricts != null && !availableDistricts.isEmpty()) {
      boolean matches = availableDistricts.stream()
          .anyMatch(d -> d.equalsIgnoreCase(district.trim()));
      if (!matches) {
        throw new IllegalArgumentException(
            "Invalid target district: '" + district + "'. Must be a recognized administrative district.");
      }
    }
  }

  private int calculateSusceptibleLivestock(String diseaseName, String district, String taluka) {
    List<Animal> localAnimals = animalRepository.findAll().stream()
        .filter(a -> a.getStatus() == null || a.getStatus() == AnimalStatus.ACTIVE)
        .filter(a -> a.getFarmer() != null && matchesDistrict(a.getFarmer().getDistrict(), district))
        .filter(a -> taluka == null || taluka.isBlank()
            || matchesDistrict(a.getFarmer().getTaluka(), taluka))
        .toList();

    int totalAnimals = localAnimals.size();
    if (totalAnimals == 0) {
      return 0;
    }

    LocalDateTime oneYearAgo = LocalDateTime.now().minusDays(365);
    LocalDate today = LocalDate.now();

    long vaccinated = localAnimals.stream()
        .filter(a -> hasValidVaccination(a, diseaseName, oneYearAgo, today))
        .count();

    int deficit = (int) Math.max(0, totalAnimals - vaccinated);
    return deficit > 0 ? deficit : totalAnimals;
  }

  private boolean matchesDistrict(String actual, String target) {
    if (actual == null || target == null) {
      return false;
    }
    return actual.trim().equalsIgnoreCase(target.trim());
  }

  private boolean hasValidVaccination(
      Animal animal, String diseaseName, LocalDateTime oneYearAgo, LocalDate today) {
    List<AnimalHealthRecord> records =
        healthRecordRepository.findByAnimalOrderByRecordedAtDesc(animal);
    return records.stream()
        .anyMatch(
            r ->
                r.getRecordType() == HealthRecordType.VACCINATION
                    && isMatchingVaccine(diseaseName, r)
                    && isVaccineValid(r, oneYearAgo, today));
  }

  private boolean isMatchingVaccine(String diseaseName, AnimalHealthRecord record) {
    if (diseaseName == null) {
      return true;
    }
    String d = diseaseName.toLowerCase(Locale.ROOT);
    String text = (record.getVaccineName() + " " + record.getTitle() + " " + record.getDescription())
        .toLowerCase(Locale.ROOT);

    if (d.contains("fmd") || d.contains("foot")) {
      return text.contains("fmd") || text.contains("foot");
    }
    if (d.contains("lsd") || d.contains("lumpy")) {
      return text.contains("lsd") || text.contains("lumpy");
    }
    if (d.contains("rabies")) {
      return text.contains("rabies");
    }
    if (d.contains("brucell")) {
      return text.contains("brucell");
    }
    if (d.contains("anthrax")) {
      return text.contains("anthrax");
    }
    return true;
  }

  private boolean isVaccineValid(
      AnimalHealthRecord record, LocalDateTime oneYearAgo, LocalDate today) {
    if (record.getNextDueDate() != null && record.getNextDueDate().isAfter(today)) {
      return true;
    }
    return record.getRecordedAt() != null && record.getRecordedAt().isAfter(oneYearAgo);
  }
}
