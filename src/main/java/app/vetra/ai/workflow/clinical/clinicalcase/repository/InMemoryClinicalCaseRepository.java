package app.vetra.ai.workflow.clinical.clinicalcase.repository;

import app.vetra.ai.workflow.clinical.clinicalcase.encounter.ClinicalEncounter;
import app.vetra.ai.workflow.clinical.clinicalcase.followup.ClinicalFollowUp;
import app.vetra.ai.workflow.clinical.clinicalcase.followup.FollowUpStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.model.ClinicalCase;
import app.vetra.ai.workflow.clinical.clinicalcase.model.ClinicalCaseStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.response.TreatmentResponse;
import app.vetra.ai.workflow.clinical.clinicalcase.timeline.ClinicalCaseTimeline;
import app.vetra.ai.workflow.clinical.clinicalcase.timeline.ClinicalTimelineEvent;
import app.vetra.ai.workflow.clinical.model.action.ClinicalActionPlan;
import app.vetra.ai.workflow.clinical.model.explainability.ClinicalDecisionSupport;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/**
 * Thread-safe, in-memory reference implementation of {@link ClinicalCaseRepository}.
 *
 * <p>Enforces append-only historical encounters and timeline events.
 */
@Repository
public class InMemoryClinicalCaseRepository implements ClinicalCaseRepository {

  private final Map<UUID, ClinicalCase> cases = new ConcurrentHashMap<>();
  private final Map<UUID, List<ClinicalEncounter>> encountersByCase = new ConcurrentHashMap<>();
  private final Map<UUID, List<ClinicalTimelineEvent>> timelineByCase = new ConcurrentHashMap<>();
  private final Map<UUID, List<ClinicalFollowUp>> followUpsByCase = new ConcurrentHashMap<>();
  private final Map<UUID, ClinicalFollowUp> followUpById = new ConcurrentHashMap<>();

  private final Map<UUID, ClinicalDecisionSupport> latestCdsByCase = new ConcurrentHashMap<>();
  private final Map<UUID, ClinicalActionPlan> latestPlanByCase = new ConcurrentHashMap<>();
  private final Map<UUID, TreatmentResponse> latestResponseByCase = new ConcurrentHashMap<>();

  @Override
  public ClinicalCase createCase(ClinicalCase clinicalCase) {
    if (clinicalCase == null) {
      throw new IllegalArgumentException("ClinicalCase cannot be null");
    }
    if (cases.containsKey(clinicalCase.caseId())) {
      throw new IllegalStateException("ClinicalCase already exists with id: " + clinicalCase.caseId());
    }
    cases.put(clinicalCase.caseId(), clinicalCase);
    encountersByCase.put(clinicalCase.caseId(), Collections.synchronizedList(new ArrayList<>()));
    timelineByCase.put(clinicalCase.caseId(), Collections.synchronizedList(new ArrayList<>()));
    followUpsByCase.put(clinicalCase.caseId(), Collections.synchronizedList(new ArrayList<>()));
    return clinicalCase;
  }

  /**
   * Helper method for testing setup to directly save a case.
   */
  public void saveCase(ClinicalCase clinicalCase) {
    if (clinicalCase != null) {
      cases.put(clinicalCase.caseId(), clinicalCase);
      encountersByCase.putIfAbsent(clinicalCase.caseId(), Collections.synchronizedList(new ArrayList<>()));
      timelineByCase.putIfAbsent(clinicalCase.caseId(), Collections.synchronizedList(new ArrayList<>()));
      followUpsByCase.putIfAbsent(clinicalCase.caseId(), Collections.synchronizedList(new ArrayList<>()));
    }
  }

  /** Helper method for testing setup to add an encounter. */
  public void addEncounter(UUID caseId, ClinicalEncounter encounter) {
    saveEncounter(encounter);
  }

  /** Helper method for testing setup to save CDS. */
  public void saveDecisionSupport(UUID caseId, ClinicalDecisionSupport cds) {
    if (caseId != null && cds != null) {
      latestCdsByCase.put(caseId, cds);
    }
  }

  /** Helper method for testing setup to save ActionPlan. */
  public void saveActionPlan(UUID caseId, ClinicalActionPlan plan) {
    if (caseId != null && plan != null) {
      latestPlanByCase.put(caseId, plan);
    }
  }

  /** Helper method for testing setup to save TreatmentResponse. */
  public void saveTreatmentResponse(UUID caseId, TreatmentResponse response) {
    if (caseId != null && response != null) {
      latestResponseByCase.put(caseId, response);
    }
  }

  @Override
  public Optional<ClinicalCase> findById(UUID caseId) {
    if (caseId == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(cases.get(caseId));
  }

  @Override
  public List<ClinicalCase> findByAnimalId(UUID animalId) {
    if (animalId == null) {
      return List.of();
    }
    return cases.values().stream()
        .filter(c -> animalId.equals(c.animalId()))
        .sorted(Comparator.comparing(ClinicalCase::openedAt).reversed())
        .toList();
  }

  @Override
  public List<ClinicalCase> findAllCases() {
    return cases.values().stream()
        .sorted(Comparator.comparing(ClinicalCase::openedAt).reversed())
        .toList();
  }

  @Override
  public ClinicalCase updateCaseStatus(UUID caseId, ClinicalCaseStatus newStatus) {
    ClinicalCase existing = findById(caseId)
        .orElseThrow(() -> new IllegalArgumentException("ClinicalCase not found with id: " + caseId));

    if (!existing.status().canTransitionTo(newStatus)) {
      throw new IllegalStateException(
          String.format("Invalid case status transition from %s to %s for caseId=%s", existing.status(), newStatus, caseId));
    }

    Instant closedAt = (newStatus == ClinicalCaseStatus.RESOLVED || newStatus == ClinicalCaseStatus.CLOSED)
        ? Instant.now()
        : existing.closedAt();

    ClinicalCase updated = new ClinicalCase(
        existing.caseId(),
        existing.animalId(),
        existing.species(),
        existing.breed(),
        existing.primaryCondition(),
        existing.openedAt(),
        Instant.now(),
        closedAt,
        newStatus);

    cases.put(caseId, updated);
    return updated;
  }

  @Override
  public ClinicalEncounter saveEncounter(ClinicalEncounter encounter) {
    if (encounter == null) {
      throw new IllegalArgumentException("ClinicalEncounter cannot be null");
    }
    List<ClinicalEncounter> encounters = encountersByCase.get(encounter.caseId());
    if (encounters == null) {
      encounters = Collections.synchronizedList(new ArrayList<>());
      encountersByCase.put(encounter.caseId(), encounters);
    }

    boolean exists = encounters.stream().anyMatch(e -> e.encounterId().equals(encounter.encounterId()));
    if (exists) {
      throw new IllegalStateException("ClinicalEncounter already exists and cannot be modified: " + encounter.encounterId());
    }

    encounters.add(encounter);

    if (encounter.decisionSupport() != null) {
      latestCdsByCase.put(encounter.caseId(), encounter.decisionSupport());
    }
    if (encounter.actionPlan() != null) {
      latestPlanByCase.put(encounter.caseId(), encounter.actionPlan());
    }

    return encounter;
  }

  @Override
  public List<ClinicalEncounter> findEncountersByCaseId(UUID caseId) {
    List<ClinicalEncounter> encounters = encountersByCase.get(caseId);
    if (encounters == null) {
      return List.of();
    }
    synchronized (encounters) {
      return encounters.stream()
          .sorted(Comparator.comparing(ClinicalEncounter::occurredAt))
          .toList();
    }
  }

  @Override
  public ClinicalTimelineEvent appendTimelineEvent(ClinicalTimelineEvent event) {
    if (event == null) {
      throw new IllegalArgumentException("ClinicalTimelineEvent cannot be null");
    }
    List<ClinicalTimelineEvent> events = timelineByCase.get(event.caseId());
    if (events == null) {
      events = Collections.synchronizedList(new ArrayList<>());
      timelineByCase.put(event.caseId(), events);
    }
    events.add(event);
    return event;
  }

  @Override
  public ClinicalCaseTimeline getTimeline(UUID caseId) {
    List<ClinicalTimelineEvent> events = timelineByCase.get(caseId);
    if (events == null) {
      return new ClinicalCaseTimeline(caseId, List.of());
    }
    synchronized (events) {
      List<ClinicalTimelineEvent> sorted = events.stream()
          .sorted(Comparator.comparing(ClinicalTimelineEvent::timestamp))
          .toList();
      return new ClinicalCaseTimeline(caseId, sorted);
    }
  }

  @Override
  public ClinicalFollowUp saveFollowUp(ClinicalFollowUp followUp) {
    if (followUp == null) {
      throw new IllegalArgumentException("ClinicalFollowUp cannot be null");
    }
    List<ClinicalFollowUp> followUps = followUpsByCase.get(followUp.caseId());
    if (followUps == null) {
      followUps = Collections.synchronizedList(new ArrayList<>());
      followUpsByCase.put(followUp.caseId(), followUps);
    }
    followUps.add(followUp);
    followUpById.put(followUp.followUpId(), followUp);
    return followUp;
  }

  @Override
  public List<ClinicalFollowUp> findFollowUpsByCaseId(UUID caseId) {
    List<ClinicalFollowUp> followUps = followUpsByCase.get(caseId);
    if (followUps == null) {
      return List.of();
    }
    synchronized (followUps) {
      return followUps.stream()
          .sorted(Comparator.comparing(ClinicalFollowUp::scheduledAt))
          .toList();
    }
  }

  @Override
  public ClinicalFollowUp updateFollowUpStatus(UUID followUpId, FollowUpStatus newStatus) {
    ClinicalFollowUp existing = followUpById.get(followUpId);
    if (existing == null) {
      throw new IllegalArgumentException("ClinicalFollowUp not found with id: " + followUpId);
    }
    Instant completedAt = (newStatus == FollowUpStatus.COMPLETED) ? Instant.now() : existing.completedAt();
    ClinicalFollowUp updated = new ClinicalFollowUp(
        existing.followUpId(),
        existing.caseId(),
        existing.sourceEncounterId(),
        existing.scheduledAt(),
        completedAt,
        newStatus,
        existing.reason(),
        existing.expectedObservations(),
        existing.escalationConditions());

    followUpById.put(followUpId, updated);

    List<ClinicalFollowUp> list = followUpsByCase.get(existing.caseId());
    if (list != null) {
      synchronized (list) {
        list.removeIf(f -> f.followUpId().equals(followUpId));
        list.add(updated);
      }
    }
    return updated;
  }

  @Override
  public Optional<ClinicalDecisionSupport> findLatestDecisionSupportByCaseId(UUID caseId) {
    if (caseId == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(latestCdsByCase.get(caseId));
  }

  @Override
  public Optional<ClinicalActionPlan> findLatestActionPlanByCaseId(UUID caseId) {
    if (caseId == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(latestPlanByCase.get(caseId));
  }

  @Override
  public Optional<TreatmentResponse> findLatestTreatmentResponseByCaseId(UUID caseId) {
    if (caseId == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(latestResponseByCase.get(caseId));
  }
}
