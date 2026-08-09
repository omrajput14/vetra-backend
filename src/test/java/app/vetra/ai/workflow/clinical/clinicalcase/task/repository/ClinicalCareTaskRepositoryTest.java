package app.vetra.ai.workflow.clinical.clinicalcase.task.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskActor;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskPriority;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskType;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.ClinicalCareTask;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClinicalCareTaskRepositoryTest {

  private InMemoryClinicalCareTaskRepository repository;

  @BeforeEach
  void setUp() {
    repository = new InMemoryClinicalCareTaskRepository();
  }

  @Test
  void testCreateAndFindTask() {
    UUID caseId = UUID.randomUUID();
    ClinicalCareTask task = new ClinicalCareTask(
        UUID.randomUUID(), caseId, UUID.randomUUID(), CareTaskType.VETERINARIAN_REVIEW, CareTaskPriority.HIGH, CareTaskActor.VETERINARIAN, CareTaskStatus.PENDING, "Title", "Desc", Instant.now(), null, null, true, true, false, List.of(), List.of(), null, null, "TEST");

    ClinicalCareTask created = repository.createTask(task);
    assertNotNull(created);
    assertEquals(task.taskId(), created.taskId());

    assertTrue(repository.findTaskById(task.taskId()).isPresent());
    List<ClinicalCareTask> found = repository.findTasksByCaseId(caseId);
    assertEquals(1, found.size());
  }

  @Test
  void testDuplicateTaskCreationThrowsException() {
    UUID caseId = UUID.randomUUID();
    ClinicalCareTask task = new ClinicalCareTask(
        UUID.randomUUID(), caseId, UUID.randomUUID(), CareTaskType.VETERINARIAN_REVIEW, CareTaskPriority.HIGH, CareTaskActor.VETERINARIAN, CareTaskStatus.PENDING, "Title", "Desc", Instant.now(), null, null, true, true, false, List.of(), List.of(), null, null, "TEST");

    repository.createTask(task);
    assertThrows(IllegalStateException.class, () -> repository.createTask(task));
  }

  @Test
  void testUpdateStatus_validAndInvalidTransitions() {
    UUID caseId = UUID.randomUUID();
    ClinicalCareTask task = repository.createTask(new ClinicalCareTask(
        UUID.randomUUID(), caseId, UUID.randomUUID(), CareTaskType.FOLLOW_UP, CareTaskPriority.MEDIUM, CareTaskActor.CAREGIVER, CareTaskStatus.PENDING, "Title", "Desc", Instant.now(), null, null, false, false, false, List.of(), List.of(), null, null, "TEST"));

    ClinicalCareTask updated = repository.updateTaskStatus(task.taskId(), CareTaskStatus.IN_PROGRESS);
    assertEquals(CareTaskStatus.IN_PROGRESS, updated.status());

    ClinicalCareTask completed = repository.updateTaskStatus(task.taskId(), CareTaskStatus.COMPLETED);
    assertEquals(CareTaskStatus.COMPLETED, completed.status());

    assertThrows(IllegalStateException.class, () -> repository.updateTaskStatus(task.taskId(), CareTaskStatus.PENDING));
  }
}
