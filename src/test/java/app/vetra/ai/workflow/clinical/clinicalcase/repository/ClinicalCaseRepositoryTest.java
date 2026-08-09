package app.vetra.ai.workflow.clinical.clinicalcase.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.ai.workflow.clinical.clinicalcase.encounter.ClinicalEncounter;
import app.vetra.ai.workflow.clinical.clinicalcase.encounter.ClinicalEncounterType;
import app.vetra.ai.workflow.clinical.clinicalcase.model.ClinicalCase;
import app.vetra.ai.workflow.clinical.clinicalcase.model.ClinicalCaseStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.timeline.ClinicalCaseTimeline;
import app.vetra.ai.workflow.clinical.clinicalcase.timeline.ClinicalTimelineEvent;
import app.vetra.ai.workflow.clinical.clinicalcase.timeline.ClinicalTimelineEventType;
import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClinicalCaseRepositoryTest {

  private InMemoryClinicalCaseRepository repository;

  @BeforeEach
  void setUp() {
    repository = new InMemoryClinicalCaseRepository();
  }

  @Test
  void testCreateAndFindCase() {
    UUID animalId = UUID.randomUUID();
    ClinicalCase c = new ClinicalCase(UUID.randomUUID(), animalId, "BOVINE", "Holstein", "Mastitis", Instant.now(), Instant.now(), null, ClinicalCaseStatus.OPEN);

    ClinicalCase created = repository.createCase(c);
    assertNotNull(created);
    assertEquals(c.caseId(), created.caseId());

    assertTrue(repository.findById(c.caseId()).isPresent());
    List<ClinicalCase> found = repository.findByAnimalId(animalId);
    assertEquals(1, found.size());
  }

  @Test
  void testUpdateCaseStatus_validAndInvalidTransitions() {
    ClinicalCase c = repository.createCase(new ClinicalCase(UUID.randomUUID(), UUID.randomUUID(), "BOVINE", "Angus", "Fever", Instant.now(), Instant.now(), null, ClinicalCaseStatus.OPEN));

    ClinicalCase updated = repository.updateCaseStatus(c.caseId(), ClinicalCaseStatus.UNDER_TREATMENT);
    assertEquals(ClinicalCaseStatus.UNDER_TREATMENT, updated.status());

    assertThrows(IllegalStateException.class, () -> repository.updateCaseStatus(c.caseId(), ClinicalCaseStatus.OPEN));
  }

  @Test
  void testSaveEncounter_appendOnlyImmutability() {
    ClinicalCase c = repository.createCase(new ClinicalCase(UUID.randomUUID(), UUID.randomUUID(), "EQUINE", "Arabian", "Colic", Instant.now(), Instant.now(), null, ClinicalCaseStatus.OPEN));

    ClinicalEncounter enc = new ClinicalEncounter(
        UUID.randomUUID(), c.caseId(), UUID.randomUUID(), Instant.now(), ClinicalEncounterType.INITIAL_ASSESSMENT, TriageUrgency.URGENT, "Colic", BigDecimal.valueOf(0.85), List.of(), null, null, null, null);

    repository.saveEncounter(enc);

    List<ClinicalEncounter> encounters = repository.findEncountersByCaseId(c.caseId());
    assertEquals(1, encounters.size());
    assertEquals(enc.encounterId(), encounters.get(0).encounterId());

    assertThrows(IllegalStateException.class, () -> repository.saveEncounter(enc));
  }

  @Test
  void testTimelineEvents_appendAndRetrieve() {
    ClinicalCase c = repository.createCase(new ClinicalCase(UUID.randomUUID(), UUID.randomUUID(), "BOVINE", "Jersey", "Ketosis", Instant.now(), Instant.now(), null, ClinicalCaseStatus.OPEN));

    ClinicalTimelineEvent event = new ClinicalTimelineEvent(UUID.randomUUID(), c.caseId(), Instant.now(), ClinicalTimelineEventType.CASE_OPENED, "Case opened", null, Map.of());
    repository.appendTimelineEvent(event);

    ClinicalCaseTimeline timeline = repository.getTimeline(c.caseId());
    assertNotNull(timeline);
    assertEquals(1, timeline.events().size());
  }
}
