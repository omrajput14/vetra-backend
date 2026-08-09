package app.vetra.ai.workflow.clinical.clinicalcase.service;

import app.vetra.ai.event.ClinicalCaseCreatedEvent;
import app.vetra.ai.event.ClinicalEncounterRecordedEvent;
import app.vetra.ai.observability.AIMetricsCollector;
import app.vetra.ai.workflow.clinical.clinicalcase.encounter.ClinicalEncounter;
import app.vetra.ai.workflow.clinical.clinicalcase.followup.ClinicalFollowUp;
import app.vetra.ai.workflow.clinical.clinicalcase.followup.FollowUpStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.model.ClinicalCase;
import app.vetra.ai.workflow.clinical.clinicalcase.model.ClinicalCaseStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.model.ClinicalCaseStatusSummary;
import app.vetra.ai.workflow.clinical.clinicalcase.repository.ClinicalCaseRepository;
import app.vetra.ai.workflow.clinical.clinicalcase.response.TreatmentResponseStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.timeline.ClinicalCaseTimeline;
import app.vetra.ai.workflow.clinical.clinicalcase.timeline.ClinicalTimelineEvent;
import app.vetra.ai.workflow.clinical.clinicalcase.timeline.ClinicalTimelineEventType;
import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Service managing longitudinal veterinary clinical cases, encounters, timeline events,
 * and lifecycle transitions.
 */
@Service
public class ClinicalCaseService {

  private static final Logger log = LoggerFactory.getLogger(ClinicalCaseService.class);

  private final ClinicalCaseRepository repository;
  private final ApplicationEventPublisher eventPublisher;
  private final AIMetricsCollector metricsCollector;

  public ClinicalCaseService(
      ClinicalCaseRepository repository,
      ApplicationEventPublisher eventPublisher,
      AIMetricsCollector metricsCollector) {
    this.repository = repository;
    this.eventPublisher = eventPublisher;
    this.metricsCollector = metricsCollector;
  }

  public ClinicalCase createCase(UUID animalId, String species, String breed, String primaryCondition) {
    if (animalId == null) {
      throw new IllegalArgumentException("animalId cannot be null");
    }

    ClinicalCase newCase = new ClinicalCase(
        UUID.randomUUID(),
        animalId,
        species,
        breed,
        primaryCondition,
        Instant.now(),
        Instant.now(),
        null,
        ClinicalCaseStatus.OPEN);

    ClinicalCase created = repository.createCase(newCase);

    repository.appendTimelineEvent(
        new ClinicalTimelineEvent(
            UUID.randomUUID(),
            created.caseId(),
            Instant.now(),
            ClinicalTimelineEventType.CASE_OPENED,
            "Clinical case opened for " + created.species() + " (" + created.primaryCondition() + ")",
            null,
            Map.of("status", created.status().name())));

    if (eventPublisher != null) {
      eventPublisher.publishEvent(new ClinicalCaseCreatedEvent(created.caseId(), animalId, Instant.now()));
    }
    if (metricsCollector != null) {
      metricsCollector.recordClinicalCase(created.status().name());
    }

    log.info("Created ClinicalCase caseId={} for animalId={}", created.caseId(), animalId);
    return created;
  }

  public ClinicalEncounter attachEncounter(UUID caseId, UUID expectedAnimalId, ClinicalEncounter encounter) {
    if (encounter == null) {
      throw new IllegalArgumentException("encounter cannot be null");
    }

    ClinicalCase clinicalCase = repository.findById(caseId)
        .orElseThrow(() -> new IllegalArgumentException("ClinicalCase not found with id: " + caseId));

    if (expectedAnimalId != null && !clinicalCase.animalId().equals(expectedAnimalId)) {
      throw new IllegalArgumentException(
          String.format("Animal ID mismatch: case animalId=%s, expected animalId=%s", clinicalCase.animalId(), expectedAnimalId));
    }

    ClinicalEncounter saved = repository.saveEncounter(encounter);

    repository.appendTimelineEvent(
        new ClinicalTimelineEvent(
            UUID.randomUUID(),
            caseId,
            saved.occurredAt(),
            ClinicalTimelineEventType.ENCOUNTER_RECORDED,
            "Encounter recorded: " + saved.primaryDiagnosis() + " (" + saved.urgency() + ")",
            saved.encounterId(),
            Map.of("urgency", saved.urgency().name(), "type", saved.type().name())));

    if (clinicalCase.status() == ClinicalCaseStatus.OPEN) {
      updateCaseStatus(caseId, ClinicalCaseStatus.UNDER_TREATMENT);
    }

    if (eventPublisher != null) {
      eventPublisher.publishEvent(new ClinicalEncounterRecordedEvent(caseId, saved.encounterId(), saved.scanId(), Instant.now()));
    }
    if (metricsCollector != null) {
      metricsCollector.recordClinicalEncounter(saved.type().name(), saved.urgency().name());
    }

    log.info("Attached ClinicalEncounter encounterId={} to caseId={}", saved.encounterId(), caseId);
    return saved;
  }

  public ClinicalCase updateCaseStatus(UUID caseId, ClinicalCaseStatus newStatus) {
    ClinicalCase updated = repository.updateCaseStatus(caseId, newStatus);

    repository.appendTimelineEvent(
        new ClinicalTimelineEvent(
            UUID.randomUUID(),
            caseId,
            Instant.now(),
            ClinicalTimelineEventType.CLINICAL_STATUS_CHANGED,
            "Case status changed to " + newStatus,
            null,
            Map.of("status", newStatus.name())));

    log.info("Updated status for caseId={} to {}", caseId, newStatus);
    return updated;
  }

  public Optional<ClinicalCase> getCase(UUID caseId) {
    return repository.findById(caseId);
  }

  public List<ClinicalCase> findCasesByAnimalId(UUID animalId) {
    return repository.findByAnimalId(animalId);
  }

  public List<ClinicalEncounter> getEncounters(UUID caseId) {
    return repository.findEncountersByCaseId(caseId);
  }

  public ClinicalCaseTimeline getTimeline(UUID caseId) {
    return repository.getTimeline(caseId);
  }

  public ClinicalCaseStatusSummary getCaseStatusSummary(UUID caseId) {
    ClinicalCase clinicalCase = repository.findById(caseId)
        .orElseThrow(() -> new IllegalArgumentException("ClinicalCase not found with id: " + caseId));

    List<ClinicalEncounter> encounters = repository.findEncountersByCaseId(caseId);
    List<ClinicalFollowUp> followUps = repository.findFollowUpsByCaseId(caseId);

    ClinicalEncounter latest = encounters.isEmpty() ? null : encounters.get(encounters.size() - 1);
    long openFollowUps = followUps.stream().filter(f -> f.status() == FollowUpStatus.SCHEDULED || f.status() == FollowUpStatus.DUE).count();
    Instant nextFollowUpAt = followUps.stream()
        .filter(f -> f.status() == FollowUpStatus.SCHEDULED)
        .map(ClinicalFollowUp::scheduledAt)
        .findFirst()
        .orElse(null);

    boolean vetRequired = latest != null && latest.decisionSupport() != null
        && latest.decisionSupport().veterinarianReviewFlag() != null
        && latest.decisionSupport().veterinarianReviewFlag().requiresReview();

    return new ClinicalCaseStatusSummary(
        caseId,
        clinicalCase.status(),
        latest != null ? latest.urgency() : TriageUrgency.ROUTINE,
        latest != null ? latest.primaryDiagnosis() : clinicalCase.primaryCondition(),
        latest != null ? latest.diagnosticConfidence() : BigDecimal.valueOf(0.10),
        TreatmentResponseStatus.INSUFFICIENT_DATA,
        vetRequired,
        (int) openFollowUps,
        latest != null ? latest.occurredAt() : clinicalCase.openedAt(),
        nextFollowUpAt);
  }
}
