package app.vetra.ai.workflow.clinical.clinicalcase.operations;

import app.vetra.ai.workflow.clinical.clinicalcase.operations.model.PageResult;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskActor;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskPriority;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.ClinicalCareTask;
import app.vetra.ai.workflow.clinical.clinicalcase.task.repository.ClinicalCareTaskRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Pure deterministic service building filtered, prioritized, and paginated care-task work queues.
 */
@Service
public class CareTaskWorkQueueService {

  private static final Logger log = LoggerFactory.getLogger(CareTaskWorkQueueService.class);
  private final ClinicalCareTaskRepository careTaskRepository;

  public CareTaskWorkQueueService(ClinicalCareTaskRepository careTaskRepository) {
    this.careTaskRepository = careTaskRepository;
  }

  /**
   * Retrieves active care-task work queue items matching filter criteria with stable pagination.
   *
   * @param actor filter by actor (optional)
   * @param priority filter by priority (optional)
   * @param status filter by status (optional)
   * @param overdueOnly if true, returns only overdue tasks
   * @param page 1-indexed page number
   * @param pageSize page size limit
   * @return paginated result of {@link ClinicalCareTaskWorkQueueItem}
   */
  public PageResult<ClinicalCareTaskWorkQueueItem> getTaskWorkQueue(
      CareTaskActor actor,
      CareTaskPriority priority,
      CareTaskStatus status,
      Boolean overdueOnly,
      int page,
      int pageSize) {

    List<ClinicalCareTask> allTasks = new ArrayList<>(careTaskRepository.findTasksByStatus(status != null ? status : CareTaskStatus.PENDING));
    if (status == null) {
      // Add assigned, in_progress, due, overdue, escalated
      List<CareTaskStatus> activeStatuses = List.of(
          CareTaskStatus.ASSIGNED, CareTaskStatus.IN_PROGRESS, CareTaskStatus.DUE,
          CareTaskStatus.OVERDUE, CareTaskStatus.ESCALATED);
      for (CareTaskStatus st : activeStatuses) {
        allTasks.addAll(careTaskRepository.findTasksByStatus(st));
      }
    }

    Instant now = Instant.now();

    List<ClinicalCareTaskWorkQueueItem> items = allTasks.stream()
        .filter(t -> actor == null || t.actor() == actor)
        .filter(t -> priority == null || t.priority() == priority)
        .filter(t -> overdueOnly == null || !overdueOnly || isOverdue(t, now))
        .map(t -> toWorkQueueItem(t, now))
        .sorted(getTaskComparator())
        .toList();

    log.debug("CareTaskWorkQueueService retrieved {} tasks (page={}, pageSize={})", items.size(), page, pageSize);
    return PageResult.of(items, page, pageSize);
  }

  private boolean isOverdue(ClinicalCareTask task, Instant now) {
    return task.status() == CareTaskStatus.OVERDUE
        || (task.dueAt() != null && task.dueAt().isBefore(now) && task.status() != CareTaskStatus.COMPLETED && task.status() != CareTaskStatus.CANCELLED);
  }

  private ClinicalCareTaskWorkQueueItem toWorkQueueItem(ClinicalCareTask t, Instant now) {
    boolean overdue = isOverdue(t, now);
    return new ClinicalCareTaskWorkQueueItem(
        t.taskId(),
        t.caseId(),
        t.sourceEncounterId(),
        t.type(),
        t.priority(),
        t.actor(),
        t.status(),
        t.title(),
        t.dueAt(),
        overdue,
        t.mandatory(),
        t.veterinarianRequired(),
        t.escalationRequired(),
        t.createdAt());
  }

  private Comparator<ClinicalCareTaskWorkQueueItem> getTaskComparator() {
    return (item1, item2) -> {
      int p1 = getPriorityRank(item1.priority());
      int p2 = getPriorityRank(item2.priority());
      if (p1 != p2) {
        return Integer.compare(p2, p1);
      }
      if (item1.escalationRequired() != item2.escalationRequired()) {
        return item1.escalationRequired() ? -1 : 1;
      }
      if (item1.veterinarianRequired() != item2.veterinarianRequired()) {
        return item1.veterinarianRequired() ? -1 : 1;
      }
      int dueCompare = compareInstants(item1.dueAt(), item2.dueAt());
      if (dueCompare != 0) {
        return dueCompare;
      }
      int createdCompare = compareInstants(item1.createdAt(), item2.createdAt());
      if (createdCompare != 0) {
        return createdCompare;
      }
      return item1.taskId().compareTo(item2.taskId());
    };
  }

  private int getPriorityRank(CareTaskPriority priority) {
    if (priority == null) {
      return 0;
    }
    return switch (priority) {
      case EMERGENCY -> 4;
      case HIGH -> 3;
      case MEDIUM -> 2;
      case LOW -> 1;
    };
  }

  private int compareInstants(Instant t1, Instant t2) {
    if (t1 == null && t2 == null) {
      return 0;
    }
    if (t1 == null) {
      return 1;
    }
    if (t2 == null) {
      return -1;
    }
    return t1.compareTo(t2);
  }
}
