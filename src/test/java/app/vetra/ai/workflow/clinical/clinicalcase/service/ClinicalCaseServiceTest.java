package app.vetra.ai.workflow.clinical.clinicalcase.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import app.vetra.ai.event.ClinicalCaseCreatedEvent;
import app.vetra.ai.event.ClinicalEncounterRecordedEvent;
import app.vetra.ai.observability.AIMetricsCollector;
import app.vetra.ai.workflow.clinical.clinicalcase.encounter.ClinicalEncounter;
import app.vetra.ai.workflow.clinical.clinicalcase.encounter.ClinicalEncounterType;
import app.vetra.ai.workflow.clinical.clinicalcase.model.ClinicalCase;
import app.vetra.ai.workflow.clinical.clinicalcase.model.ClinicalCaseStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.repository.InMemoryClinicalCaseRepository;
import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class ClinicalCaseServiceTest {

  private InMemoryClinicalCaseRepository repository;
  private ApplicationEventPublisher eventPublisher;
  private AIMetricsCollector metricsCollector;
  private ClinicalCaseService service;

  @BeforeEach
  void setUp() {
    repository = new InMemoryClinicalCaseRepository();
    eventPublisher = mock(ApplicationEventPublisher.class);
    metricsCollector = mock(AIMetricsCollector.class);
    service = new ClinicalCaseService(repository, eventPublisher, metricsCollector);
  }

  @Test
  void testCreateCase_publishesEventAndMetrics() {
    UUID animalId = UUID.randomUUID();
    ClinicalCase created = service.createCase(animalId, "BOVINE", "Holstein", "Mastitis");

    assertNotNull(created);
    assertEquals(animalId, created.animalId());
    assertEquals(ClinicalCaseStatus.OPEN, created.status());

    verify(eventPublisher).publishEvent(any(ClinicalCaseCreatedEvent.class));
    verify(metricsCollector).recordClinicalCase("OPEN");
  }

  @Test
  void testAttachEncounter_validatesAnimalIdMismatch() {
    UUID animalId = UUID.randomUUID();
    ClinicalCase created = service.createCase(animalId, "BOVINE", "Angus", "Fever");

    UUID wrongAnimalId = UUID.randomUUID();
    ClinicalEncounter encounter =
        new ClinicalEncounter(
            UUID.randomUUID(),
            created.caseId(),
            UUID.randomUUID(),
            Instant.now(),
            ClinicalEncounterType.INITIAL_ASSESSMENT,
            TriageUrgency.URGENT,
            "Fever",
            BigDecimal.valueOf(0.80),
            List.of(),
            null,
            null,
            null,
            null);

    assertThrows(
        IllegalArgumentException.class,
        () -> service.attachEncounter(created.caseId(), wrongAnimalId, encounter));
  }

  @Test
  void testAttachEncounter_success() {
    UUID animalId = UUID.randomUUID();
    ClinicalCase created = service.createCase(animalId, "BOVINE", "Holstein", "Pneumonia");

    ClinicalEncounter encounter =
        new ClinicalEncounter(
            UUID.randomUUID(),
            created.caseId(),
            UUID.randomUUID(),
            Instant.now(),
            ClinicalEncounterType.INITIAL_ASSESSMENT,
            TriageUrgency.URGENT,
            "Bovine Respiratory Disease",
            BigDecimal.valueOf(0.90),
            List.of(),
            null,
            null,
            null,
            null);

    ClinicalEncounter enc = service.attachEncounter(created.caseId(), animalId, encounter);

    assertNotNull(enc);
    assertEquals(created.caseId(), enc.caseId());
    verify(eventPublisher).publishEvent(any(ClinicalEncounterRecordedEvent.class));
  }
}
