package app.vetra.ai.workflow.clinical.clinicalcase.task.repository;

import app.vetra.ai.workflow.clinical.clinicalcase.coordination.FollowUpSchedule;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskAssignment;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.ClinicalCareTask;
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
 * Thread-safe reference implementation of {@link ClinicalCareTaskRepository}.
 *
 * <p>Preserves historical completed and escalated tasks for full auditability.
 */
@Repository
public class InMemoryClinicalCareTaskRepository implements ClinicalCareTaskRepository {

  private final Map<UUID, ClinicalCareTask> tasksById = new ConcurrentHashMap<>();
  private final Map<UUID, List<ClinicalCareTask>> tasksByCase = new ConcurrentHashMap<>();
  private final Map<UUID, List<CareTaskAssignment>> assignmentsByTask = new ConcurrentHashMap<>();
  private final Map<UUID, List<FollowUpSchedule>> schedulesByCase = new ConcurrentHashMap<>();

  @Override
  public ClinicalCareTask createTask(ClinicalCareTask task) {
    if (task == null) {
      throw new IllegalArgumentException("ClinicalCareTask cannot be null");
    }
    if (tasksById.containsKey(task.taskId())) {
      throw new IllegalStateException("ClinicalCareTask already exists with id: " + task.taskId());
    }

    tasksById.put(task.taskId(), task);
    tasksByCase.computeIfAbsent(task.caseId(), k -> Collections.synchronizedList(new ArrayList<>())).add(task);
    return task;
  }

  @Override
  public Optional<ClinicalCareTask> findTaskById(UUID taskId) {
    if (taskId == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(tasksById.get(taskId));
  }

  @Override
  public List<ClinicalCareTask> findTasksByCaseId(UUID caseId) {
    List<ClinicalCareTask> list = tasksByCase.get(caseId);
    if (list == null) {
      return List.of();
    }
    synchronized (list) {
      return list.stream()
          .sorted(Comparator.comparing(ClinicalCareTask::createdAt))
          .toList();
    }
  }

  @Override
  public List<ClinicalCareTask> findTasksByStatus(CareTaskStatus status) {
    if (status == null) {
      return List.of();
    }
    return tasksById.values().stream()
        .filter(t -> t.status() == status)
        .sorted(Comparator.comparing(ClinicalCareTask::createdAt))
        .toList();
  }

  @Override
  public List<ClinicalCareTask> findTasksDueBefore(Instant timestamp) {
    if (timestamp == null) {
      return List.of();
    }
    return tasksById.values().stream()
        .filter(t -> t.dueAt() != null && t.dueAt().isBefore(timestamp) && t.status() != CareTaskStatus.COMPLETED && t.status() != CareTaskStatus.CANCELLED)
        .sorted(Comparator.comparing(ClinicalCareTask::dueAt))
        .toList();
  }

  @Override
  public List<ClinicalCareTask> findOverdueTasks(Instant currentTimestamp) {
    Instant now = currentTimestamp != null ? currentTimestamp : Instant.now();
    return tasksById.values().stream()
        .filter(t -> (t.status() == CareTaskStatus.OVERDUE)
            || (t.dueAt() != null && t.dueAt().isBefore(now) && t.status() != CareTaskStatus.COMPLETED && t.status() != CareTaskStatus.CANCELLED && t.status() != CareTaskStatus.ESCALATED))
        .sorted(Comparator.comparing(ClinicalCareTask::dueAt))
        .toList();
  }

  @Override
  public ClinicalCareTask updateTaskStatus(UUID taskId, CareTaskStatus newStatus) {
    ClinicalCareTask existing = findTaskById(taskId)
        .orElseThrow(() -> new IllegalArgumentException("ClinicalCareTask not found with id: " + taskId));

    if (!existing.status().canTransitionTo(newStatus)) {
      throw new IllegalStateException(
          String.format("Invalid task status transition from %s to %s for taskId=%s", existing.status(), newStatus, taskId));
    }

    Instant completedAt = (newStatus == CareTaskStatus.COMPLETED) ? Instant.now() : existing.completedAt();
    ClinicalCareTask updated = new ClinicalCareTask(
        existing.taskId(),
        existing.caseId(),
        existing.sourceEncounterId(),
        existing.type(),
        existing.priority(),
        existing.actor(),
        newStatus,
        existing.title(),
        existing.description(),
        existing.createdAt(),
        existing.dueAt(),
        completedAt,
        existing.mandatory(),
        existing.veterinarianRequired(),
        existing.escalationRequired(),
        existing.supportingEvidence(),
        existing.supportingCitations(),
        existing.sourceActionId(),
        existing.sourceFollowUpId(),
        existing.provenance());

    tasksById.put(taskId, updated);

    List<ClinicalCareTask> caseList = tasksByCase.get(existing.caseId());
    if (caseList != null) {
      synchronized (caseList) {
        caseList.removeIf(t -> t.taskId().equals(taskId));
        caseList.add(updated);
      }
    }
    return updated;
  }

  @Override
  public CareTaskAssignment assignTask(CareTaskAssignment assignment) {
    if (assignment == null) {
      throw new IllegalArgumentException("CareTaskAssignment cannot be null");
    }
    assignmentsByTask.computeIfAbsent(assignment.taskId(), k -> Collections.synchronizedList(new ArrayList<>())).add(assignment);
    return assignment;
  }

  @Override
  public List<CareTaskAssignment> findAssignmentsByTaskId(UUID taskId) {
    List<CareTaskAssignment> list = assignmentsByTask.get(taskId);
    if (list == null) {
      return List.of();
    }
    synchronized (list) {
      return list.stream()
          .sorted(Comparator.comparing(CareTaskAssignment::assignedAt))
          .toList();
    }
  }

  @Override
  public FollowUpSchedule saveFollowUpSchedule(FollowUpSchedule schedule) {
    if (schedule == null) {
      throw new IllegalArgumentException("FollowUpSchedule cannot be null");
    }
    schedulesByCase.computeIfAbsent(schedule.caseId(), k -> Collections.synchronizedList(new ArrayList<>())).add(schedule);
    return schedule;
  }

  @Override
  public List<FollowUpSchedule> findFollowUpSchedulesByCaseId(UUID caseId) {
    List<FollowUpSchedule> list = schedulesByCase.get(caseId);
    if (list == null) {
      return List.of();
    }
    synchronized (list) {
      return list.stream()
          .sorted(Comparator.comparing(FollowUpSchedule::scheduledAt))
          .toList();
    }
  }
}
