package app.vetra.disease.service;

import app.vetra.auth.repository.FarmerProfileRepository;
import app.vetra.auth.repository.UserRepository;
import app.vetra.disease.dto.OperationalAlertResponse;
import app.vetra.disease.entity.AlertAction;
import app.vetra.disease.entity.Outbreak;
import app.vetra.disease.repository.AlertActionRepository;
import app.vetra.disease.repository.OutbreakRepository;
import app.vetra.infrastructure.exception.ResourceNotFoundException;
import app.vetra.infrastructure.persistence.entity.FarmerProfile;
import app.vetra.notification.service.AreaNotificationService;
import app.vetra.vaccination.dto.CreateVaccinationCampaignRequest;
import app.vetra.vaccination.dto.VaccinationCampaignResponse;
import app.vetra.vaccination.entity.VaccinationCampaign;
import app.vetra.vaccination.enums.CampaignPriority;
import app.vetra.vaccination.enums.CampaignStatus;
import app.vetra.vaccination.repository.VaccinationCampaignRepository;
import app.vetra.vaccination.service.VaccinationCampaignService;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Officer actions from the dashboard: deploy containment, acknowledge or escalate an alert. */
@Service
public class SurveillanceActionService {

  private static final Logger log = LoggerFactory.getLogger(SurveillanceActionService.class);
  private static final double VET_RADIUS_KM = 50.0;

  private final OutbreakRepository outbreakRepository;
  private final VaccinationCampaignService campaignService;
  private final VaccinationCampaignRepository campaignRepository;
  private final FarmerProfileRepository farmerProfileRepository;
  private final AreaNotificationService areaNotificationService;
  private final AlertActionRepository alertActionRepository;
  private final OperationalAlertService operationalAlertService;
  private final UserRepository userRepository;

  public SurveillanceActionService(
      OutbreakRepository outbreakRepository,
      VaccinationCampaignService campaignService,
      VaccinationCampaignRepository campaignRepository,
      FarmerProfileRepository farmerProfileRepository,
      AreaNotificationService areaNotificationService,
      AlertActionRepository alertActionRepository,
      OperationalAlertService operationalAlertService,
      UserRepository userRepository) {
    this.outbreakRepository = outbreakRepository;
    this.campaignService = campaignService;
    this.campaignRepository = campaignRepository;
    this.farmerProfileRepository = farmerProfileRepository;
    this.areaNotificationService = areaNotificationService;
    this.alertActionRepository = alertActionRepository;
    this.operationalAlertService = operationalAlertService;
    this.userRepository = userRepository;
  }

  /** Optional settings for a containment order; all fields may be null. */
  public record ContainmentRequest(Integer plannedDoses, String targetDistrict, LocalDate startDate, String notes) {}

  /** What the containment order did. */
  public record ContainmentResult(
      UUID campaignId, String campaignName, boolean campaignCreated, String campaignNote,
      int farmersNotified, int vetsNotified, int paraVetsNotified, double radiusKm) {}

  /**
   * Launches (or reuses) a ring-vaccination campaign for the outbreak and pushes an advisory to
   * farmers inside the outbreak radius and vets within 50 km.
   */
  @Transactional
  public ContainmentResult deployContainment(String officer, UUID outbreakId, ContainmentRequest req) {
    Outbreak o = outbreakRepository.findById(outbreakId)
        .orElseThrow(() -> new ResourceNotFoundException("Outbreak not found: " + outbreakId, "DIS_404"));
    double lat = o.getCenterLatitude();
    double lng = o.getCenterLongitude();
    double radius = o.getRadiusKm() != null ? o.getRadiusKm() : 15.0;
    LocalDate start = req != null && req.startDate() != null ? req.startDate() : LocalDate.now().plusDays(1);

    UUID campaignId = null;
    String campaignName = null;
    boolean created = false;
    String campaignNote;
    List<VaccinationCampaign> open = campaignRepository.findByOutbreakIdAndStatusIn(
        outbreakId, List.of(CampaignStatus.PLANNED, CampaignStatus.ACTIVE));
    if (!open.isEmpty()) {
      campaignId = open.get(0).getId();
      campaignName = open.get(0).getCampaignName();
      campaignNote = "A ring-vaccination campaign is already running for this outbreak.";
    } else {
      String district = req != null && req.targetDistrict() != null && !req.targetDistrict().isBlank()
          ? req.targetDistrict() : nearestFarmDistrict(lat, lng);
      campaignName = o.getDiseaseName() + " ring vaccination - " + district + " " + start.getYear();
      try {
        VaccinationCampaignResponse c = campaignService.createCampaign(officer,
            new CreateVaccinationCampaignRequest(
                campaignName.length() > 150 ? campaignName.substring(0, 150) : campaignName,
                o.getDiseaseName(), district, null, null,
                req != null && req.plannedDoses() != null ? req.plannedDoses() : 500,
                CampaignPriority.CRITICAL, start, start.plusDays(30), outbreakId,
                "Containment ordered from the government dashboard"
                    + (req != null && req.notes() != null ? ": " + req.notes() : ".")));
        campaignId = c.id();
        created = true;
        campaignNote = "Ring-vaccination campaign launched.";
      } catch (IllegalArgumentException e) {
        campaignName = null;
        campaignNote = "No campaign launched: " + e.getMessage();
        log.warn("[CONTAINMENT] campaign not created for outbreak {}: {}", outbreakId, e.getMessage());
      }
    }

    String disease = o.getDiseaseName();
    String payload = "{\"outbreakId\":\"" + outbreakId + "\",\"route\":\"/outbreaks\"}";
    // Farmers inside the outbreak ring; vets over a wider area, since they serve many villages.
    int farmers = areaNotificationService.notifyWithin(lat, lng, radius,
        disease + " outbreak near your farm",
        "Cases of " + disease + " are confirmed within " + Math.round(radius) + " km of your farm."
            + " Keep sick animals apart and call your vet. Ring vaccination starts " + start + ".",
        null, null, payload).farmers();
    int vets = areaNotificationService.notifyWithin(lat, lng, Math.max(radius, VET_RADIUS_KM), null, null,
        "Containment ordered: " + disease,
        "The district office ordered ring vaccination for a " + disease + " outbreak near you from "
            + start + ". Please support the drive.",
        payload).vets();
    int paraVets = areaNotificationService.notifyParaVetsWithin(lat, lng, Math.max(radius, VET_RADIUS_KM),
        "Vaccination drive: " + disease,
        "Ring vaccination for a " + disease + " outbreak near you starts " + start
            + ". Open Vaccination drives in the app to record the doses you give.",
        "{\"campaignId\":\"" + campaignId + "\",\"route\":\"/paravet-drives\"}");
    log.info("[CONTAINMENT] outbreak={} campaign={} farmers={} vets={} paraVets={}",
        outbreakId, campaignId, farmers, vets, paraVets);
    return new ContainmentResult(campaignId, campaignName, created, campaignNote, farmers, vets, paraVets, radius);
  }

  /** Result of an alert action. */
  public record AlertActionResult(UUID alertId, String status, int vetsNotified) {}

  /** Marks an alert acknowledged (stored, so it survives reloads and other officers see it). */
  @Transactional
  public AlertActionResult acknowledge(String officer, UUID alertId, String note) {
    operationalAlertService.getAlertById(alertId);
    save(officer, alertId, "ACKNOWLEDGED", note);
    return new AlertActionResult(alertId, "ACKNOWLEDGED", 0);
  }

  /** Escalates an alert: stored, and vets within 50 km of it are pushed an urgent notice. */
  @Transactional
  public AlertActionResult escalate(String officer, UUID alertId, String note) {
    OperationalAlertResponse a = operationalAlertService.getAlertById(alertId);
    save(officer, alertId, "ESCALATED", note);
    int vets = 0;
    if (a.latitude() != null && a.longitude() != null) {
      vets = areaNotificationService.notifyWithin(a.latitude(), a.longitude(), VET_RADIUS_KM, null, null,
          "Escalated: " + a.title(),
          "The district office escalated this alert. " + a.recommendedNextStep(),
          "{\"alertId\":\"" + alertId + "\",\"route\":\"/outbreaks\"}").vets();
    }
    return new AlertActionResult(alertId, "ESCALATED", vets);
  }

  private void save(String officer, UUID alertId, String status, String note) {
    AlertAction action = alertActionRepository.findByAlertId(alertId).orElseGet(AlertAction::new);
    action.setAlertId(alertId);
    action.setStatus(status);
    action.setNote(note);
    userRepository.findByIdentifier(officer).ifPresent(u -> action.setActorUserId(u.getId()));
    alertActionRepository.save(action);
  }

  private String nearestFarmDistrict(double lat, double lng) {
    return farmerProfileRepository.findAll().stream()
        .filter(f -> f.getLatitude() != null && f.getLongitude() != null && f.getDistrict() != null)
        .min(Comparator.comparingDouble((FarmerProfile f) ->
            Math.pow(f.getLatitude() - lat, 2) + Math.pow(f.getLongitude() - lng, 2)))
        .map(FarmerProfile::getDistrict)
        .orElse("Maharashtra");
  }
}
