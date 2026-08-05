package app.vetra.disease.service;

import app.vetra.ai.entity.AIScan;
import app.vetra.ai.entity.AIScanStatus;
import app.vetra.ai.repository.AIScanRepository;
import app.vetra.animal.repository.AnimalRepository;
import app.vetra.auth.repository.UserRepository;
import app.vetra.disease.dto.CreateDiseaseReportRequest;
import app.vetra.disease.dto.DiseaseReportResponse;
import app.vetra.disease.dto.NearbyReportResponse;
import app.vetra.disease.dto.OutbreakResponse;
import app.vetra.disease.dto.OutbreakStatisticsResponse;
import app.vetra.disease.engine.OutbreakDetectionEngine;
import app.vetra.disease.entity.DiagnosisConfidenceSource;
import app.vetra.disease.entity.DiagnosisStatus;
import app.vetra.disease.entity.DiseaseReport;
import app.vetra.disease.entity.DiseaseReportSource;
import app.vetra.disease.entity.Outbreak;
import app.vetra.disease.entity.OutbreakRiskScore;
import app.vetra.disease.entity.OutbreakStatus;
import app.vetra.disease.event.DiseaseConfirmedEvent;
import app.vetra.disease.event.DiseaseReportCreatedEvent;
import app.vetra.disease.geo.GeoUtils;
import app.vetra.disease.repository.DiseaseReportRepository;
import app.vetra.disease.repository.OutbreakRepository;
import app.vetra.infrastructure.cache.CacheNames;
import app.vetra.infrastructure.exception.BusinessRuleException;
import app.vetra.infrastructure.exception.ResourceNotFoundException;
import app.vetra.infrastructure.exception.UnauthorizedResourceAccessException;
import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.entity.MedicalRecord;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.enums.UserRole;
import app.vetra.medicalrecord.repository.MedicalRecordRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core business service managing disease reports, PostGIS spatial queries, and outbreak detection
 * intelligence.
 */
@Service
public class DiseaseService {

  private static final Logger log = LoggerFactory.getLogger(DiseaseService.class);

  private final DiseaseReportRepository diseaseReportRepository;
  private final OutbreakRepository outbreakRepository;
  private final AnimalRepository animalRepository;
  private final UserRepository userRepository;
  private final MedicalRecordRepository medicalRecordRepository;
  private final AIScanRepository aiScanRepository;
  private final OutbreakDetectionEngine outbreakDetectionEngine;
  private final ApplicationEventPublisher eventPublisher;

  /** Constructor injection. */
  public DiseaseService(
      DiseaseReportRepository diseaseReportRepository,
      OutbreakRepository outbreakRepository,
      AnimalRepository animalRepository,
      UserRepository userRepository,
      MedicalRecordRepository medicalRecordRepository,
      AIScanRepository aiScanRepository,
      OutbreakDetectionEngine outbreakDetectionEngine,
      ApplicationEventPublisher eventPublisher) {
    this.diseaseReportRepository = diseaseReportRepository;
    this.outbreakRepository = outbreakRepository;
    this.animalRepository = animalRepository;
    this.userRepository = userRepository;
    this.medicalRecordRepository = medicalRecordRepository;
    this.aiScanRepository = aiScanRepository;
    this.outbreakDetectionEngine = outbreakDetectionEngine;
    this.eventPublisher = eventPublisher;
  }

  /**
   * Submits a new disease surveillance report.
   *
   * @param userIdentifier email or phone of reporting user
   * @param request creation request parameters
   * @return {@link DiseaseReportResponse}
   */
  @Transactional
  @CacheEvict(
      value = {
        CacheNames.DASHBOARD_VET,
        CacheNames.DASHBOARD_ADMIN,
        CacheNames.OUTBREAKS,
        CacheNames.ANALYTICS
      },
      allEntries = true)
  public DiseaseReportResponse createReport(
      String userIdentifier, CreateDiseaseReportRequest request) {
    User user = getUserByEmailOrPhone(userIdentifier);
    Animal animal =
        animalRepository
            .findById(request.animalId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Animal not found with ID: " + request.animalId(), "ANIMAL_001"));

    // Business Rule: Unverified AI scans cannot generate disease reports directly
    AIScan aiScan = null;
    if (request.aiScanId() != null) {
      aiScan =
          aiScanRepository
              .findById(request.aiScanId())
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          "AI scan not found: " + request.aiScanId(), "AI_001"));

      if (aiScan.getStatus() != AIScanStatus.VERIFIED) {
        throw new BusinessRuleException(
            "Unverified AI diagnostic predictions cannot generate disease reports directly",
            "DISEASE_001");
      }
    }

    if (request.reportSource() == DiseaseReportSource.AI_VERIFIED && aiScan == null) {
      throw new BusinessRuleException(
          "AI_VERIFIED report source requires a verified AI scan reference", "DISEASE_002");
    }

    MedicalRecord medicalRecord = null;
    if (request.medicalRecordId() != null) {
      medicalRecord =
          medicalRecordRepository
              .findById(request.medicalRecordId())
              .orElseThrow(
                  () -> new ResourceNotFoundException("Medical record not found", "MEDICAL_001"));
    }

    DiagnosisConfidenceSource confidenceSource =
        request.diagnosisConfidenceSource() != null
            ? request.diagnosisConfidenceSource()
            : (request.reportSource() == DiseaseReportSource.AI_VERIFIED
                ? DiagnosisConfidenceSource.AI_VERIFIED
                : DiagnosisConfidenceSource.VETERINARIAN);

    DiseaseReport report =
        DiseaseReport.builder()
            .animal(animal)
            .medicalRecord(medicalRecord)
            .aiScan(aiScan)
            .reportedBy(user)
            .reportSource(request.reportSource())
            .diagnosisConfidenceSource(confidenceSource)
            .diseaseName(request.diseaseName().trim())
            .diagnosisStatus(request.diagnosisStatus())
            .latitude(request.latitude())
            .longitude(request.longitude())
            .notes(request.notes() != null ? request.notes().trim() : null)
            .build();

    report = diseaseReportRepository.save(report);

    log.info(
        "Disease report created id={} disease='{}' status={} confidence={} lat={} lng={}",
        report.getId(),
        report.getDiseaseName(),
        report.getDiagnosisStatus(),
        report.getDiagnosisConfidenceSource(),
        report.getLatitude(),
        report.getLongitude());

    eventPublisher.publishEvent(
        new DiseaseReportCreatedEvent(
            report.getId(),
            animal.getId(),
            report.getDiseaseName(),
            report.getDiagnosisStatus(),
            report.getLatitude(),
            report.getLongitude()));

    if (report.getDiagnosisStatus() == DiagnosisStatus.CONFIRMED) {
      eventPublisher.publishEvent(
          new DiseaseConfirmedEvent(
              report.getId(),
              animal.getId(),
              report.getDiseaseName(),
              report.getLatitude(),
              report.getLongitude()));
      outbreakDetectionEngine.evaluateReport(report);
    }

    return DiseaseReportResponse.fromEntity(report);
  }

  /** Retrieves a disease report by ID with ownership assertion for farmers. */
  @Transactional(readOnly = true)
  @Cacheable(
      value = CacheNames.DISEASE_REPORTS,
      key = "T(app.vetra.infrastructure.cache.CacheKeys).diseaseReportKey(#reportId)")
  public DiseaseReportResponse getReport(String userIdentifier, UUID reportId) {
    User user = getUserByEmailOrPhone(userIdentifier);
    DiseaseReport report =
        diseaseReportRepository
            .findById(reportId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Disease report not found with ID: " + reportId, "DISEASE_003"));

    if (user.getRole() == UserRole.FARMER) {
      if (!report.getAnimal().getFarmer().getUser().getId().equals(user.getId())
          && !report.getReportedBy().getId().equals(user.getId())) {
        throw new UnauthorizedResourceAccessException(
            "Farmers can only view disease reports for their own livestock", "DISEASE_004");
      }
    }

    return DiseaseReportResponse.fromEntity(report);
  }

  /** Lists disease reports with pagination based on active user role. */
  @Transactional(readOnly = true)
  public Page<DiseaseReportResponse> listReports(String userIdentifier, Pageable pageable) {
    User user = getUserByEmailOrPhone(userIdentifier);

    if (user.getRole() == UserRole.FARMER) {
      return diseaseReportRepository
          .findByReportedById(user.getId(), pageable)
          .map(DiseaseReportResponse::fromEntity);
    } else {
      return diseaseReportRepository.findAll(pageable).map(DiseaseReportResponse::fromEntity);
    }
  }

  /** Searches for disease reports within a geographic radius in kilometers. */
  @Transactional(readOnly = true)
  public List<NearbyReportResponse> searchNearbyReports(
      Double latitude, Double longitude, Double radiusKm) {
    double radius = (radiusKm != null && radiusKm > 0) ? radiusKm : 25.0;

    double latDelta = radius / 111.0;
    double lonDelta = radius / (111.0 * Math.cos(Math.toRadians(latitude)));

    double minLat = latitude - latDelta;
    double maxLat = latitude + latDelta;
    double minLon = longitude - lonDelta;
    double maxLon = longitude + lonDelta;

    List<DiseaseReport> reports =
        diseaseReportRepository.findWithinBoundingBox(minLat, maxLat, minLon, maxLon);

    return reports.stream()
        .map(
            r -> {
              double dist =
                  GeoUtils.calculateDistanceKm(
                      latitude, longitude, r.getLatitude(), r.getLongitude());
              return NearbyReportResponse.from(r, dist);
            })
        .filter(nr -> nr.distanceKm() <= radius)
        .toList();
  }

  /** Lists active or all outbreaks. */
  @Transactional(readOnly = true)
  public List<OutbreakResponse> listOutbreaks(OutbreakStatus status) {
    List<Outbreak> outbreaks;
    if (status != null) {
      outbreaks = outbreakRepository.findByStatusOrderByCreatedAtDesc(status);
    } else {
      outbreaks = outbreakRepository.findAll();
    }
    return outbreaks.stream().map(OutbreakResponse::fromEntity).toList();
  }

  /** Retrieves high-risk outbreak clusters (HIGH or CRITICAL severity). */
  @Transactional(readOnly = true)
  public List<OutbreakResponse> getHighRiskOutbreaks() {
    return outbreakRepository.findAll().stream()
        .filter(o -> o.getStatus() != OutbreakStatus.RESOLVED)
        .filter(
            o ->
                o.getRiskScore() == OutbreakRiskScore.HIGH
                    || o.getRiskScore() == OutbreakRiskScore.CRITICAL)
        .map(OutbreakResponse::fromEntity)
        .toList();
  }

  /** Generates summary outbreak epidemiological statistics. */
  @Transactional(readOnly = true)
  public OutbreakStatisticsResponse getOutbreakStatistics() {
    List<Outbreak> all = outbreakRepository.findAll();
    long total = all.size();
    long active =
        all.stream()
            .filter(
                o ->
                    o.getStatus() == OutbreakStatus.ACTIVE
                        || o.getStatus() == OutbreakStatus.DETECTED)
            .count();
    long critical =
        all.stream()
            .filter(
                o ->
                    o.getRiskScore() == OutbreakRiskScore.CRITICAL
                        && o.getStatus() != OutbreakStatus.RESOLVED)
            .count();
    long high =
        all.stream()
            .filter(
                o ->
                    o.getRiskScore() == OutbreakRiskScore.HIGH
                        && o.getStatus() != OutbreakStatus.RESOLVED)
            .count();
    long totalAffected = all.stream().mapToLong(Outbreak::getAffectedReportsCount).sum();

    return new OutbreakStatisticsResponse(total, active, critical, high, totalAffected);
  }

  /** Retrieves all disease reports contributing to a specific outbreak cluster. */
  @Transactional(readOnly = true)
  public List<DiseaseReportResponse> getReportsForOutbreak(UUID outbreakId) {
    Outbreak outbreak =
        outbreakRepository
            .findById(outbreakId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Outbreak cluster not found with ID: " + outbreakId, "DISEASE_005"));

    Instant cutoff =
        outbreak.getCreatedAt().minus(outbreak.getEvaluationWindowHours(), ChronoUnit.HOURS);

    return diseaseReportRepository
        .findByDiseaseNameIgnoreCaseAndDiagnosisStatusOrderByCreatedAtDesc(
            outbreak.getDiseaseName(), DiagnosisStatus.CONFIRMED)
        .stream()
        .filter(r -> r.getCreatedAt().isAfter(cutoff))
        .filter(
            r ->
                GeoUtils.calculateDistanceKm(
                        outbreak.getCenterLatitude(),
                        outbreak.getCenterLongitude(),
                        r.getLatitude(),
                        r.getLongitude())
                    <= outbreak.getRadiusKm())
        .map(DiseaseReportResponse::fromEntity)
        .toList();
  }

  /** Retrieves an outbreak by ID. */
  @Transactional(readOnly = true)
  public OutbreakResponse getOutbreak(UUID id) {
    Outbreak outbreak =
        outbreakRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Outbreak cluster not found with ID: " + id, "DISEASE_005"));
    return OutbreakResponse.fromEntity(outbreak);
  }

  private User getUserByEmailOrPhone(String identifier) {
    return userRepository
        .findByEmail(identifier)
        .or(() -> userRepository.findByPhone(identifier))
        .orElseThrow(
            () -> new ResourceNotFoundException("User not found: " + identifier, "USER_004"));
  }
}
