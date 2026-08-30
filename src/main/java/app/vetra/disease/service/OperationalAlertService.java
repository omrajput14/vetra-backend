package app.vetra.disease.service;

import app.vetra.disease.dto.OperationalAlertResponse;
import app.vetra.disease.entity.DiagnosisConfidenceSource;
import app.vetra.disease.entity.DiseaseReport;
import app.vetra.disease.entity.Outbreak;
import app.vetra.disease.entity.OutbreakRiskScore;
import app.vetra.disease.entity.OutbreakStatus;
import app.vetra.disease.repository.DiseaseReportRepository;
import app.vetra.disease.repository.OutbreakRepository;
import app.vetra.disease.risk.vaccination.VaccinationGapService;
import app.vetra.infrastructure.exception.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service dynamically synthesizing operational surveillance alerts from current active
 * outbreak clusters and field surveillance reports.
 *
 * <p>Operational Architecture Note:
 * 1. Alerts are derived dynamically on-the-fly from active surveillance entities.
 * 2. Deterministic UUIDs provide stable, idempotent identities across repeated evaluation cycles.
 * 3. Alerts are ephemeral surveillance telemetry, NOT persisted incident records in a database.
 * 4. There is currently no persistent acknowledge/resolve workflow; alerts exist as long as the
 *    underlying surveillance conditions persist.
 */
@Service
public class OperationalAlertService {

  private final OutbreakRepository outbreakRepository;
  private final DiseaseReportRepository diseaseReportRepository;
  private final VaccinationGapService vaccinationGapService;

  /** Constructor injection. */
  public OperationalAlertService(
      OutbreakRepository outbreakRepository,
      DiseaseReportRepository diseaseReportRepository,
      VaccinationGapService vaccinationGapService) {
    this.outbreakRepository = outbreakRepository;
    this.diseaseReportRepository = diseaseReportRepository;
    this.vaccinationGapService = vaccinationGapService;
  }

  /**
   * Evaluates active outbreaks and field reports to produce a deterministic operational alert list.
   *
   * @return List of {@link OperationalAlertResponse} sorted by severity and recency
   */
  @Transactional(readOnly = true)
  public List<OperationalAlertResponse> listOperationalAlerts() {
    List<Outbreak> activeOutbreaks =
        outbreakRepository.findAll().stream()
            .filter(o -> o.getStatus() != OutbreakStatus.RESOLVED)
            .toList();

    List<DiseaseReport> allReports = diseaseReportRepository.findAll();
    List<OperationalAlertResponse> alerts = new ArrayList<>();

    for (Outbreak outbreak : activeOutbreaks) {
      Double score =
          outbreak.getCompositeRiskScore() != null
              ? outbreak.getCompositeRiskScore().doubleValue()
              : null;
      Double vacGap = outbreak.getVaccinationGapScore();
      Integer cases = outbreak.getAffectedReportsCount();
      String loc =
          String.format(
              "Lat %.3f, Lng %.3f (Radius %.1f km)",
              outbreak.getCenterLatitude(),
              outbreak.getCenterLongitude(),
              outbreak.getRadiusKm());

      checkCriticalAlert(outbreak, score, vacGap, cases, loc, alerts);
      checkImmunityGapAlert(outbreak, score, vacGap, cases, loc, alerts);
      checkCaseVolumeAlert(outbreak, score, vacGap, cases, loc, alerts);
      checkLabConfirmedAlert(outbreak, score, vacGap, cases, loc, allReports, alerts);
    }

    alerts.sort(
        Comparator.comparingInt(
                (OperationalAlertResponse a) -> severityOrder(a.severity()))
            .thenComparing(
                Comparator.comparing(OperationalAlertResponse::detectedAt).reversed()));

    return alerts;
  }

  private void checkCriticalAlert(
      Outbreak o,
      Double score,
      Double vacGap,
      Integer cases,
      String loc,
      List<OperationalAlertResponse> list) {
    if (o.getRiskScore() == OutbreakRiskScore.CRITICAL || (score != null && score >= 80.0)) {
      String why;
      if (score != null && cases != null) {
        why = String.format("Multi-signal risk score reached %.1f/100 with %d active reports.", score, cases);
      } else if (score != null) {
        why = String.format("Multi-signal risk score reached %.1f/100.", score);
      } else if (cases != null) {
        why = String.format("Critical outbreak severity registered with %d active reports (risk score unavailable).", cases);
      } else {
        why = "Critical outbreak severity registered (risk score and case count unavailable).";
      }

      list.add(
          new OperationalAlertResponse(
              generateDeterministicUuid(o.getId(), "CRITICAL"),
              "CRITICAL_OUTBREAK_DETECTED",
              "Critical Outbreak Alert: " + o.getDiseaseName(),
              o.getDiseaseName(),
              loc,
              o.getCenterLatitude(),
              o.getCenterLongitude(),
              "CRITICAL",
              score,
              vacGap,
              cases,
              o.getCreatedAt(),
              "MultiSignalRiskEngine",
              o.getStatus().name(),
              why,
              "Execute statutory bio-quarantine perimeter and dispatch rapid response veterinary team.",
              o.getId(),
              null));
    }
  }

  private void checkImmunityGapAlert(
      Outbreak o,
      Double score,
      Double vacGap,
      Integer cases,
      String loc,
      List<OperationalAlertResponse> list) {
    if (vacGap != null
        && vacGap >= 50.0
        && (o.getRiskScore() == OutbreakRiskScore.HIGH || (score != null && score >= 60.0))) {
      list.add(
          new OperationalAlertResponse(
              generateDeterministicUuid(o.getId(), "IMMUNITY_GAP"),
              "IMMUNITY_GAP_OVERLAP",
              "Severe Immunity Deficit Overlap: " + o.getDiseaseName(),
              o.getDiseaseName(),
              loc,
              o.getCenterLatitude(),
              o.getCenterLongitude(),
              score != null && score >= 75.0 ? "CRITICAL" : "HIGH",
              score,
              vacGap,
              cases,
              o.getUpdatedAt() != null ? o.getUpdatedAt() : o.getCreatedAt(),
              "VaccinationGapService",
              "ACTION_REQUIRED",
              String.format("Vaccination gap score reaches %.1f/100 in %.1f km perimeter.", vacGap, o.getRadiusKm()),
              "Prioritize block-level ring vaccination to create an immune barrier.",
              o.getId(),
              null));
    }
  }

  private void checkCaseVolumeAlert(
      Outbreak o,
      Double score,
      Double vacGap,
      Integer cases,
      String loc,
      List<OperationalAlertResponse> list) {
    if (cases != null && cases >= 3) {
      list.add(
          new OperationalAlertResponse(
              generateDeterministicUuid(o.getId(), "CASE_VOLUME"),
              "CASE_VOLUME_ESCALATION",
              "Case Volume Escalation: " + o.getDiseaseName(),
              o.getDiseaseName(),
              loc,
              o.getCenterLatitude(),
              o.getCenterLongitude(),
              "HIGH",
              score,
              vacGap,
              cases,
              o.getUpdatedAt() != null ? o.getUpdatedAt() : o.getCreatedAt(),
              "OutbreakDetectionEngine",
              o.getStatus().name(),
              String.format("High case volume (%d cases) detected within clustering window.", cases),
              "Conduct door-to-door herd health screening across all farms within containment buffer.",
              o.getId(),
              null));
    }
  }

  private void checkLabConfirmedAlert(
      Outbreak o,
      Double score,
      Double vacGap,
      Integer cases,
      String loc,
      List<DiseaseReport> reports,
      List<OperationalAlertResponse> list) {
    boolean hasLabConfirmed =
        reports.stream()
            .filter(r -> r.getDiseaseName().equalsIgnoreCase(o.getDiseaseName()))
            .anyMatch(r -> r.getDiagnosisConfidenceSource() == DiagnosisConfidenceSource.LAB_CONFIRMED);

    if (hasLabConfirmed) {
      list.add(
          new OperationalAlertResponse(
              generateDeterministicUuid(o.getId(), "LAB_CONFIRMED"),
              "LAB_CONFIRMED_CLUSTER",
              "Accredited Lab Confirmation: " + o.getDiseaseName(),
              o.getDiseaseName(),
              loc,
              o.getCenterLatitude(),
              o.getCenterLongitude(),
              "HIGH",
              score,
              vacGap,
              cases,
              o.getCreatedAt(),
              "VeterinarySurveillance",
              "VERIFIED",
              "Diagnostic laboratory assay (LAB_CONFIRMED) registered on clinical health record for this pathogen.",
              "Enforce movement restrictions for livestock in the infected taluka/block.",
              o.getId(),
              null));
    }
  }

  /**
   * Retrieves a single operational alert by its deterministic ID.
   *
   * @param id alert UUID
   * @return {@link OperationalAlertResponse}
   */
  @Transactional(readOnly = true)
  public OperationalAlertResponse getAlertById(UUID id) {
    return listOperationalAlerts().stream()
        .filter(a -> a.id().equals(id))
        .findFirst()
        .orElseThrow(
            () -> new ResourceNotFoundException("Operational alert not found with id: " + id));
  }

  private int severityOrder(String severity) {
    return switch (severity.toUpperCase()) {
      case "CRITICAL" -> 1;
      case "HIGH" -> 2;
      case "MEDIUM" -> 3;
      case "LOW" -> 4;
      default -> 5;
    };
  }

  private UUID generateDeterministicUuid(UUID baseUuid, String type) {
    return UUID.nameUUIDFromBytes((baseUuid.toString() + "-" + type).getBytes());
  }
}
