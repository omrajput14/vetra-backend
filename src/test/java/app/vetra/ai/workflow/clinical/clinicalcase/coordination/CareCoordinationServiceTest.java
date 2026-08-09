package app.vetra.ai.workflow.clinical.clinicalcase.coordination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import app.vetra.ai.event.ClinicalCareTaskAssignedEvent;
import app.vetra.ai.event.ClinicalCareTaskCompletedEvent;
import app.vetra.ai.event.ClinicalCareTaskCreatedEvent;
import app.vetra.ai.observability.AIMetricsCollector;
import app.vetra.ai.workflow.clinical.clinicalcase.encounter.ClinicalEncounter;
import app.vetra.ai.workflow.clinical.clinicalcase.encounter.ClinicalEncounterType;
import app.vetra.ai.workflow.clinical.clinicalcase.model.ClinicalCase;
import app.vetra.ai.workflow.clinical.clinicalcase.model.ClinicalCaseStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.repository.InMemoryClinicalCaseRepository;
import app.vetra.ai.workflow.clinical.clinicalcase.task.engine.CareTaskEscalationEngine;
import app.vetra.ai.workflow.clinical.clinicalcase.task.engine.ClinicalCareTaskEngine;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskActor;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskAssignment;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskSummary;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.ClinicalCareTask;
import app.vetra.ai.workflow.clinical.clinicalcase.task.repository.InMemoryClinicalCareTaskRepository;
import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class CareCoordinationServiceTest {

  private InMemoryClinicalCaseRepository caseRepository;
  private InMemoryClinicalCareTaskRepository taskRepository;
  private ClinicalCareTaskEngine taskEngine;
  private CareTaskEscalationEngine escalationEngine;
  private ApplicationEventPublisher eventPublisher;
  private AIMetricsCollector metricsCollector;
  private CareCoordinationService service;

  @BeforeEach
  void setUp() {
    caseRepository = new InMemoryClinicalCaseRepository();
    taskRepository = new InMemoryClinicalCareTaskRepository();
    taskEngine = new ClinicalCareTaskEngine();
    escalationEngine = new CareTaskEscalationEngine();
    eventPublisher = mock(ApplicationEventPublisher.class);
    metricsCollector = mock(AIMetricsCollector.class);

    service = new CareCoordinationService(
        caseRepository, taskRepository, taskEngine, escalationEngine, eventPublisher, metricsCollector);
  }

  @Test
  void testOrchestrateCareTasks_createsNewTasksAndPublishesEvents() {
    UUID animalId = UUID.randomUUID();
    ClinicalCase c = caseRepository.createCase(new ClinicalCase(UUID.randomUUID(), animalId, "BOVINE", "Holstein", "Mastitis", Instant.now(), Instant.now(), null, ClinicalCaseStatus.OPEN));

    ClinicalEncounter enc = new ClinicalEncounter(
        UUID.randomUUID(), c.caseId(), UUID.randomUUID(), Instant.now(), ClinicalEncounterType.INITIAL_ASSESSMENT, TriageUrgency.EMERGENCY, "Acute Mastitis", BigDecimal.valueOf(0.95), List.of(), null, null, null, null);

    List<ClinicalCareTask> tasks = service.orchestrateCareTasks(c.caseId(), enc, null, null, null, null);

    assertNotNull(tasks);
    assertFalse(tasks.isEmpty());
    verify(eventPublisher).publishEvent(any(ClinicalCareTaskCreatedEvent.class));
    verify(metricsCollector).recordCareTaskCreated(any(), any(), any());
  }

  @Test
  void testAssignAndCompleteTask() {
    UUID animalId = UUID.randomUUID();
    ClinicalCase c = caseRepository.createCase(new ClinicalCase(UUID.randomUUID(), animalId, "BOVINE", "Holstein", "Mastitis", Instant.now(), Instant.now(), null, ClinicalCaseStatus.OPEN));

    ClinicalEncounter enc = new ClinicalEncounter(
        UUID.randomUUID(), c.caseId(), UUID.randomUUID(), Instant.now(), ClinicalEncounterType.INITIAL_ASSESSMENT, TriageUrgency.EMERGENCY, "Acute Mastitis", BigDecimal.valueOf(0.95), List.of(), null, null, null, null);

    List<ClinicalCareTask> tasks = service.orchestrateCareTasks(c.caseId(), enc, null, null, null, null);
    ClinicalCareTask task = tasks.get(0);

    CareTaskAssignment assignment = service.assignTask(task.taskId(), CareTaskActor.VETERINARIAN, "Dr. Smith");
    assertNotNull(assignment);
    verify(eventPublisher).publishEvent(any(ClinicalCareTaskAssignedEvent.class));

    ClinicalCareTask completed = service.completeTask(task.taskId(), "Dr. Smith");
    assertEquals(CareTaskStatus.COMPLETED, completed.status());
    verify(eventPublisher).publishEvent(any(ClinicalCareTaskCompletedEvent.class));
  }

  @Test
  void testGetCareTaskSummary() {
    UUID animalId = UUID.randomUUID();
    ClinicalCase c = caseRepository.createCase(new ClinicalCase(UUID.randomUUID(), animalId, "BOVINE", "Holstein", "Mastitis", Instant.now(), Instant.now(), null, ClinicalCaseStatus.OPEN));

    ClinicalEncounter enc = new ClinicalEncounter(
        UUID.randomUUID(), c.caseId(), UUID.randomUUID(), Instant.now(), ClinicalEncounterType.INITIAL_ASSESSMENT, TriageUrgency.EMERGENCY, "Acute Mastitis", BigDecimal.valueOf(0.95), List.of(), null, null, null, null);

    service.orchestrateCareTasks(c.caseId(), enc, null, null, null, null);
    CareTaskSummary summary = service.getCareTaskSummary(c.caseId());

    assertNotNull(summary);
    assertEquals(c.caseId(), summary.caseId());
    assertTrue(summary.totalTasks() > 0);
  }

  private void assertTrue(boolean condition) {
    org.junit.jupiter.api.Assertions.assertTrue(condition);
  }
}
