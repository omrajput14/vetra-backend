package app.vetra.vaccination.service;

import app.vetra.ai.service.AIScanTriageService;
import app.vetra.animal.repository.AnimalHealthRecordRepository;
import app.vetra.animal.repository.AnimalRepository;
import app.vetra.disease.entity.Outbreak;
import app.vetra.infrastructure.exception.BusinessRuleException;
import app.vetra.infrastructure.exception.ResourceNotFoundException;
import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.entity.AnimalHealthRecord;
import app.vetra.infrastructure.persistence.entity.FarmerProfile;
import app.vetra.infrastructure.persistence.entity.ParaVetProfile;
import app.vetra.infrastructure.persistence.enums.HealthRecordSource;
import app.vetra.infrastructure.persistence.enums.HealthRecordType;
import app.vetra.notification.service.AreaNotificationService;
import app.vetra.vaccination.entity.VaccinationCampaign;
import app.vetra.vaccination.enums.CampaignStatus;
import app.vetra.vaccination.repository.VaccinationCampaignRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vaccination drives for para-vets: the open campaigns in their area, the animals inside a drive's
 * area, and recording the doses they give. Each dose is a VACCINATION health record (so coverage
 * and immunity-gap analytics move) and raises the campaign's administered-dose count.
 */
// ponytail: filters campaigns, animals and records in memory; move to queries past ~100k animals.
@Service
public class ParaVetDriveService {

  private static final Logger log = LoggerFactory.getLogger(ParaVetDriveService.class);
  private static final double NEAR_KM = 50.0;

  private final VaccinationCampaignRepository campaignRepository;
  private final AnimalRepository animalRepository;
  private final AnimalHealthRecordRepository healthRecordRepository;
  private final AIScanTriageService triageService;

  public ParaVetDriveService(
      VaccinationCampaignRepository campaignRepository,
      AnimalRepository animalRepository,
      AnimalHealthRecordRepository healthRecordRepository,
      AIScanTriageService triageService) {
    this.campaignRepository = campaignRepository;
    this.animalRepository = animalRepository;
    this.healthRecordRepository = healthRecordRepository;
    this.triageService = triageService;
  }

  /** A campaign as a para-vet sees it. */
  public record Drive(
      UUID id, String campaignName, String diseaseName, CampaignStatus status, int plannedDoses,
      int administeredDoses, LocalDate startDate, String targetDistrict) {}

  /** An animal inside a drive's area. */
  public record DriveAnimal(
      UUID animalId, String animalName, String tagNumber, String species, String farmerName, String village) {}

  /** Result of recording doses. */
  public record DoseResult(int recorded, int administeredDoses, int plannedDoses) {}

  /** Planned and active campaigns in the para-vet's district or within 50 km of them. */
  @Transactional(readOnly = true)
  public List<Drive> openDrives(String identifier) {
    ParaVetProfile p = approved(identifier);
    return campaignRepository.findAll().stream()
        .filter(c -> c.getStatus() == CampaignStatus.PLANNED || c.getStatus() == CampaignStatus.ACTIVE)
        .filter(c -> inParaVetArea(c, p))
        .map(c -> new Drive(c.getId(), c.getCampaignName(), c.getDiseaseName(), c.getStatus(),
            c.getPlannedDoses(), c.getAdministeredDoses(), c.getStartDate(), c.getTargetDistrict()))
        .toList();
  }

  /** Animals inside the drive's area that have not had a dose in this drive yet. */
  @Transactional(readOnly = true)
  public List<DriveAnimal> animalsToVaccinate(String identifier, UUID campaignId) {
    approved(identifier);
    VaccinationCampaign c = campaign(campaignId);
    Set<UUID> done = dosedAnimalIds(campaignId);
    return animalRepository.findAll().stream()
        .filter(a -> !done.contains(a.getId()) && inDriveArea(a.getFarmer(), c))
        .map(a -> new DriveAnimal(a.getId(), a.getAnimalName(), a.getTagNumber(),
            a.getSpecies() != null ? a.getSpecies().name() : null,
            a.getFarmer().getFullName(), a.getFarmer().getVillage()))
        .toList();
  }

  /** Records one dose per animal (animals already dosed in this drive are skipped). */
  @Transactional
  public DoseResult recordDoses(String identifier, UUID campaignId, Collection<UUID> animalIds) {
    ParaVetProfile p = approved(identifier);
    VaccinationCampaign c = campaign(campaignId);
    if (c.getStatus() != CampaignStatus.PLANNED && c.getStatus() != CampaignStatus.ACTIVE) {
      throw new BusinessRuleException("This vaccination drive is closed", "VAC_010");
    }
    Set<UUID> done = dosedAnimalIds(campaignId);
    int recorded = 0;
    for (Animal a : animalRepository.findAllById(animalIds)) {
      if (done.contains(a.getId()) || !inDriveArea(a.getFarmer(), c)) {
        continue;
      }
      AnimalHealthRecord r = new AnimalHealthRecord();
      r.setAnimal(a);
      r.setRecordType(HealthRecordType.VACCINATION);
      r.setSource(HealthRecordSource.SYSTEM);
      r.setTitle(c.getDiseaseName() + " vaccination (ring drive)");
      r.setVaccineName(c.getDiseaseName() + " vaccine");
      r.setDescription("Dose given in " + c.getCampaignName() + " by para-vet " + p.getFullName());
      r.setVeterinarianName(p.getFullName() + " (para-vet)");
      r.setNextDueDate(LocalDate.now().plusYears(1));
      r.setRecordedAt(LocalDateTime.now());
      r.setCampaignId(c.getId());
      r.setAdministeredBy(p.getUser().getId());
      healthRecordRepository.save(r);
      recorded++;
    }
    if (recorded > 0) {
      c.setAdministeredDoses(c.getAdministeredDoses() + recorded);
      if (c.getStatus() == CampaignStatus.PLANNED) {
        c.setStatus(CampaignStatus.ACTIVE);
      }
      campaignRepository.save(c);
    }
    log.info("[DRIVE] campaign={} paraVet={} recorded={} total={}", c.getId(), p.getId(), recorded, c.getAdministeredDoses());
    return new DoseResult(recorded, c.getAdministeredDoses(), c.getPlannedDoses());
  }

  private ParaVetProfile approved(String identifier) {
    triageService.requireApprovedParaVet(identifier);
    return triageService.paraVetProfile(identifier);
  }

  private VaccinationCampaign campaign(UUID id) {
    return campaignRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Vaccination campaign not found", "VAC_404"));
  }

  private Set<UUID> dosedAnimalIds(UUID campaignId) {
    return healthRecordRepository.findAll().stream()
        .filter(r -> campaignId.equals(r.getCampaignId()))
        .map(r -> r.getAnimal().getId())
        .collect(Collectors.toSet());
  }

  /** Campaign is in the para-vet's district, or its outbreak is within 50 km of them. */
  private static boolean inParaVetArea(VaccinationCampaign c, ParaVetProfile p) {
    if (p.getDistrict() == null || p.getDistrict().isBlank()) {
      return true;
    }
    if (p.getDistrict().trim().equalsIgnoreCase(c.getTargetDistrict())) {
      return true;
    }
    Outbreak o = c.getOutbreak();
    return o != null && AreaNotificationService.inside(
        p.getLatitude(), p.getLongitude(), o.getCenterLatitude(), o.getCenterLongitude(), NEAR_KM);
  }

  /** Inside the outbreak ring for outbreak campaigns, else in the campaign's district. */
  private static boolean inDriveArea(FarmerProfile farm, VaccinationCampaign c) {
    if (farm == null) {
      return false;
    }
    Outbreak o = c.getOutbreak();
    if (o != null && o.getCenterLatitude() != null) {
      double radius = o.getRadiusKm() != null ? o.getRadiusKm() : 15.0;
      return AreaNotificationService.inside(
          farm.getLatitude(), farm.getLongitude(), o.getCenterLatitude(), o.getCenterLongitude(), radius);
    }
    return c.getTargetDistrict() != null && c.getTargetDistrict().equalsIgnoreCase(farm.getDistrict());
  }
}
