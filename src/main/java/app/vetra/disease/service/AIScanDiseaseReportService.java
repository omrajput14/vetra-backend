package app.vetra.disease.service;

import app.vetra.ai.entity.AIScan;
import app.vetra.disease.entity.DiagnosisConfidenceSource;
import app.vetra.disease.entity.DiagnosisStatus;
import app.vetra.disease.entity.DiseaseReport;
import app.vetra.disease.entity.DiseaseReportSource;
import app.vetra.disease.engine.OutbreakDetectionEngine;
import app.vetra.disease.event.DiseaseConfirmedEvent;
import app.vetra.disease.event.DiseaseReportCreatedEvent;
import app.vetra.disease.repository.DiseaseReportRepository;
import app.vetra.infrastructure.cache.CacheNames;
import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.entity.FarmerProfile;
import app.vetra.infrastructure.persistence.entity.MedicalRecord;
import app.vetra.infrastructure.persistence.entity.VetProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dedicated service that creates a CONFIRMED {@link DiseaseReport} from a veterinarian-approved
 * AI diagnostic scan and immediately triggers outbreak cluster evaluation.
 *
 * <p>Extracted from {@link DiseaseService} to keep that class within the 500-line Checkstyle
 * limit and to avoid a circular Spring dependency between {@code AIScanService} and
 * {@code DiseaseService} (which already depends on {@code AIScanRepository}).
 *
 * <p>GPS resolution order:
 * <ol>
 *   <li>Vet profile coordinates (updated during en-route appointment tracking)
 *   <li>Farmer profile coordinates (registered farm location)
 *   <li>(0.0, 0.0) fallback with a WARN log so ops can identify missing-location animals
 * </ol>
 */
@Service
public class AIScanDiseaseReportService {

  private static final Logger log = LoggerFactory.getLogger(AIScanDiseaseReportService.class);

  private final DiseaseReportRepository diseaseReportRepository;
  private final OutbreakDetectionEngine outbreakDetectionEngine;
  private final ApplicationEventPublisher eventPublisher;

  /** Constructor injection. */
  public AIScanDiseaseReportService(
      DiseaseReportRepository diseaseReportRepository,
      OutbreakDetectionEngine outbreakDetectionEngine,
      ApplicationEventPublisher eventPublisher) {
    this.diseaseReportRepository = diseaseReportRepository;
    this.outbreakDetectionEngine = outbreakDetectionEngine;
    this.eventPublisher = eventPublisher;
  }

  /**
   * Creates a CONFIRMED DiseaseReport tied to the given verified AI scan, then fires
   * {@link OutbreakDetectionEngine#evaluateReport(DiseaseReport)} identical to the manual-report
   * CONFIRMED branch in {@code DiseaseService.createReport()}.
   *
   * @param scan         the VERIFIED AIScan entity
   * @param vetProfile   the approving vet (GPS source and reporter identity)
   * @param medicalRecord the MedicalRecord already saved from the same approval action
   * @return the saved DiseaseReport
   */
  @Transactional
  @CacheEvict(
      value = {CacheNames.DISEASE_REPORTS, CacheNames.OUTBREAKS, CacheNames.ANALYTICS},
      allEntries = true)
  public DiseaseReport createConfirmedReport(
      AIScan scan, VetProfile vetProfile, MedicalRecord medicalRecord) {

    Animal animal = scan.getAnimal();

    // GPS: prefer vet's last-known position (updated during en-route tracking),
    // fall back to the farmer's registered farm coordinates.
    double lat = 0.0;
    double lng = 0.0;
    if (vetProfile.getLatitude() != null && vetProfile.getLongitude() != null) {
      lat = vetProfile.getLatitude();
      lng = vetProfile.getLongitude();
    } else {
      FarmerProfile farmer = animal.getFarmer();
      if (farmer != null && farmer.getLatitude() != null && farmer.getLongitude() != null) {
        lat = farmer.getLatitude();
        lng = farmer.getLongitude();
      } else {
        log.warn(
            "[AI-SCAN APPROVAL] No GPS on vet or farmer for animalId={} scanId={}."
                + " DiseaseReport stored at (0,0) pin will not appear on map.",
            animal.getId(),
            scan.getId());
      }
    }

    String diseaseName =
        (scan.getDiagnosis() != null && !scan.getDiagnosis().isBlank())
            ? scan.getDiagnosis().trim()
            : "Unspecified Condition (AI-Scan)";

    DiseaseReport report =
        DiseaseReport.builder()
            .animal(animal)
            .medicalRecord(medicalRecord)
            .aiScan(scan)
            .reportedBy(vetProfile.getUser())
            .reportSource(DiseaseReportSource.AI_VERIFIED)
            .diagnosisConfidenceSource(DiagnosisConfidenceSource.AI_VERIFIED)
            .diseaseName(diseaseName)
            .diagnosisStatus(DiagnosisStatus.CONFIRMED)
            .latitude(lat)
            .longitude(lng)
            .notes("Auto-generated from vet-approved AI scan " + scan.getId())
            .build();

    report = diseaseReportRepository.save(report);

    log.info(
        "[AI-SCAN APPROVAL] DiseaseReport CONFIRMED id={} disease='{}' lat={} lng={} scanId={}",
        report.getId(),
        diseaseName,
        lat,
        lng,
        scan.getId());

    eventPublisher.publishEvent(
        new DiseaseReportCreatedEvent(
            report.getId(), animal.getId(), diseaseName, DiagnosisStatus.CONFIRMED, lat, lng));

    eventPublisher.publishEvent(
        new DiseaseConfirmedEvent(report.getId(), animal.getId(), diseaseName, lat, lng));

    // Mirrors DiseaseService.createReport() — triggers outbreak cluster analysis.
    outbreakDetectionEngine.evaluateReport(report);

    return report;
  }
}
