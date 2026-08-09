package app.vetra.ai.workflow.clinical.clinicalcase.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.ai.workflow.clinical.clinicalcase.operations.model.PageResult;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskActor;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskPriority;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskType;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.ClinicalCareTask;
import app.vetra.ai.workflow.clinical.clinicalcase.task.repository.InMemoryClinicalCareTaskRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CareTaskWorkQueueServiceTest {

  private InMemoryClinicalCareTaskRepository repository;
  private CareTaskWorkQueueService service;

  @BeforeEach
  void setUp() {
    repository = new InMemoryClinicalCareTaskRepository();
    service = new CareTaskWorkQueueService(repository);
  }

  @Test
  void testGetTaskWorkQueue_filteringAndEmergencyOrdering() {
    UUID caseId = UUID.randomUUID();
    Instant now = Instant.now();

    ClinicalCareTask tRoutine = new ClinicalCareTask(
        UUID.randomUUID(), caseId, UUID.randomUUID(), CareTaskType.MONITORING, CareTaskPriority.LOW, CareTaskActor.CAREGIVER, CareTaskStatus.PENDING, "Routine Check", "Desc", now, now.plusSeconds(86400), null, false, false, false, List.of(), List.of(), null, null, "TEST");

    ClinicalCareTask tEmergency = new ClinicalCareTask(
        UUID.randomUUID(), caseId, UUID.randomUUID(), CareTaskType.EMERGENCY_ESCALATION, CareTaskPriority.EMERGENCY, CareTaskActor.VETERINARIAN, CareTaskStatus.PENDING, "Emergency Action", "Desc", now, now, null, true, true, true, List.of(), List.of(), null, null, "TEST");

    repository.createTask(tRoutine);
    repository.createTask(tEmergency);

    PageResult<ClinicalCareTaskWorkQueueItem> page = service.getTaskWorkQueue(null, null, null, false, 1, 10);

    assertNotNull(page);
    assertEquals(2, page.totalItems());
    assertEquals(tEmergency.taskId(), page.items().get(0).taskId());
    assertEquals(CareTaskPriority.EMERGENCY, page.items().get(0).priority());
  }

  @Test
  void testGetTaskWorkQueue_actorFilteringAndPagination() {
    UUID caseId = UUID.randomUUID();
    Instant now = Instant.now();

    ClinicalCareTask t1 = new ClinicalCareTask(
        UUID.randomUUID(), caseId, UUID.randomUUID(), CareTaskType.VETERINARIAN_REVIEW, CareTaskPriority.HIGH, CareTaskActor.VETERINARIAN, CareTaskStatus.PENDING, "Vet Task 1", "Desc", now, now.plusSeconds(3600), null, true, true, false, List.of(), List.of(), null, null, "TEST");

    ClinicalCareTask t2 = new ClinicalCareTask(
        UUID.randomUUID(), caseId, UUID.randomUUID(), CareTaskType.TREATMENT_REVIEW, CareTaskPriority.MEDIUM, CareTaskActor.CAREGIVER, CareTaskStatus.PENDING, "Farmer Task 1", "Desc", now, now.plusSeconds(7200), null, false, false, false, List.of(), List.of(), null, null, "TEST");

    repository.createTask(t1);
    repository.createTask(t2);

    PageResult<ClinicalCareTaskWorkQueueItem> vetPage = service.getTaskWorkQueue(CareTaskActor.VETERINARIAN, null, null, false, 1, 10);
    assertEquals(1, vetPage.totalItems());
    assertEquals(CareTaskActor.VETERINARIAN, vetPage.items().get(0).actor());
  }
}
