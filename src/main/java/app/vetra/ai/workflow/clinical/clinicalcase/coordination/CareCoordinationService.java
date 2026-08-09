package app.vetra.ai.workflow.clinical.clinicalcase.coordination;

import app.vetra.ai.event.ClinicalCareTaskAssignedEvent;
import app.vetra.ai.event.ClinicalCareTaskCompletedEvent;
import app.vetra.ai.event.ClinicalCareTaskCreatedEvent;
import app.vetra.ai.event.ClinicalCareTaskEscalatedEvent;
import app.vetra.ai.event.ClinicalCareTaskOverdueEvent;
import app.vetra.ai.observability.AIMetricsCollector;
import app.vetra.ai.workflow.clinical.clinicalcase.encounter.ClinicalEncounter;
import app.vetra.ai.workflow.clinical.clinicalcase.followup.ClinicalFollowUp;
import app.vetra.ai.workflow.clinical.clinicalcase.model.ClinicalCase;
import app.vetra.ai.workflow.clinical.clinicalcase.repository.ClinicalCaseRepository;
import app.vetra.ai.workflow.clinical.clinicalcase.response.TreatmentResponse;
import app.vetra.ai.workflow.clinical.clinicalcase.task.engine.CareTaskEscalationEngine;
import app.vetra.ai.workflow.clinical.clinicalcase.task.engine.ClinicalCareTaskEngine;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskActor;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskAssignment;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskSummary;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.ClinicalCareTask;
import app.vetra.ai.workflow.clinical.clinicalcase.task.repository.ClinicalCareTaskRepository;
import app.vetra.ai.workflow.clinical.clinicalcase.timeline.ClinicalTimelineEvent;
import app.vetra.ai.workflow.clinical.clinicalcase.timeline.ClinicalTimelineEventType;
import app.vetra.ai.workflow.clinical.model.action.ClinicalActionPlan;
import app.vetra.ai.workflow.clinical.model.explainability.ClinicalDecisionSupport;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Service orchestrating clinical care tasks, assignments, follow-up scheduling,
 * lifecycle transitions, overdue evaluation, and escalation events.
 */
@Service
public class CareCoordinationService {

  private static final Logger log = LoggerFactory.getLogger(CareCoordinationService.class);

  private final ClinicalCaseRepository caseRepository;
  private final ClinicalCareTaskRepository taskRepository;
  private final ClinicalCareTaskEngine taskEngine;
  private final CareTaskEscalationEngine escalationEngine;
  private final ApplicationEventPublisher eventPublisher;
  private final AIMetricsCollector metricsCollector;

  public CareCoordinationService(
      ClinicalCaseRepository caseRepository,
      ClinicalCareTaskRepository taskRepository,
      ClinicalCareTaskEngine taskEngine,
      CareTaskEscalationEngine escalationEngine,
      ApplicationEventPublisher eventPublisher,
      AIMetricsCollector metricsCollector) {
    this.caseRepository = caseRepository;
    this.taskRepository = taskRepository;
    this.taskEngine = taskEngine;
    this.escalationEngine = escalationEngine;
    this.eventPublisher = eventPublisher;
    this.metricsCollector = metricsCollector;
  }

  public List<ClinicalCareTask> orchestrateCareTasks(
      UUID caseId,
      ClinicalEncounter encounter,
      ClinicalActionPlan actionPlan,
      ClinicalDecisionSupport decisionSupport,
      List<ClinicalFollowUp> followUps,
      TreatmentResponse treatmentResponse) {

    ClinicalCase cCase = caseRepository.findById(caseId)
        .orElseThrow(() -> new IllegalArgumentException("ClinicalCase not found with id: " + caseId));

    List<ClinicalCareTask> existingTasks = taskRepository.findTasksByCaseId(caseId);
    List<ClinicalCareTask> generatedTasks = taskEngine.generateCareTasks(
        cCase, encounter, actionPlan, decisionSupport, followUps, treatmentResponse);

    List<ClinicalCareTask> createdTasks = new ArrayList<>();

    for (ClinicalCareTask genTask : generatedTasks) {
      if (!isTaskDuplicate(genTask, existingTasks)) {
        ClinicalCareTask created = taskRepository.createTask(genTask);
        createdTasks.add(created);

        caseRepository.appendTimelineEvent(
            new ClinicalTimelineEvent(
                UUID.randomUUID(),
                caseId,
                created.createdAt(),
                ClinicalTimelineEventType.CARE_TASK_CREATED,
                "Care task created: " + created.title() + " (" + created.priority() + ")",
                created.sourceEncounterId(),
                Map.of("taskId", created.taskId().toString(), "type", created.type().name(), "priority", created.priority().name())));

        if (eventPublisher != null) {
          eventPublisher.publishEvent(
              new ClinicalCareTaskCreatedEvent(
                  caseId, created.taskId(), created.type(), created.priority(), created.actor(), Instant.now()));
        }
        if (metricsCollector != null) {
          metricsCollector.recordCareTaskCreated(created.type().name(), created.priority().name(), created.actor().name());
        }
      }
    }

    log.info("Orchestrated care tasks for caseId={}: created {} new tasks.", caseId, createdTasks.size());
    return taskRepository.findTasksByCaseId(caseId);
  }

  public CareTaskAssignment assignTask(UUID taskId, CareTaskActor actor, String assignedBy) {
    ClinicalCareTask task = taskRepository.findTaskById(taskId)
        .orElseThrow(() -> new IllegalArgumentException("ClinicalCareTask not found with id: " + taskId));

    CareTaskAssignment assignment = new CareTaskAssignment(
        UUID.randomUUID(), taskId, actor, Instant.now(), assignedBy != null ? assignedBy : "SYSTEM", null, null);

    taskRepository.assignTask(assignment);
    taskRepository.updateTaskStatus(taskId, CareTaskStatus.ASSIGNED);

    caseRepository.appendTimelineEvent(
        new ClinicalTimelineEvent(
            UUID.randomUUID(),
            task.caseId(),
            assignment.assignedAt(),
            ClinicalTimelineEventType.CARE_TASK_ASSIGNED,
            "Care task assigned to " + actor + ": " + task.title(),
            task.sourceEncounterId(),
            Map.of("taskId", taskId.toString(), "actor", actor.name())));

    if (eventPublisher != null) {
      eventPublisher.publishEvent(new ClinicalCareTaskAssignedEvent(task.caseId(), taskId, actor, Instant.now()));
    }
    return assignment;
  }

  public ClinicalCareTask completeTask(UUID taskId, String completedBy) {
    ClinicalCareTask task = taskRepository.findTaskById(taskId)
        .orElseThrow(() -> new IllegalArgumentException("ClinicalCareTask not found with id: " + taskId));

    ClinicalCareTask completed = taskRepository.updateTaskStatus(taskId, CareTaskStatus.COMPLETED);

    caseRepository.appendTimelineEvent(
        new ClinicalTimelineEvent(
            UUID.randomUUID(),
            task.caseId(),
            Instant.now(),
            ClinicalTimelineEventType.CARE_TASK_COMPLETED,
            "Care task completed: " + task.title(),
            task.sourceEncounterId(),
            Map.of("taskId", taskId.toString(), "actor", task.actor().name())));

    if (eventPublisher != null) {
      eventPublisher.publishEvent(new ClinicalCareTaskCompletedEvent(task.caseId(), taskId, task.actor(), Instant.now()));
    }
    if (metricsCollector != null) {
      metricsCollector.recordCareTaskCompleted(task.type().name(), task.priority().name(), task.actor().name());
    }
    return completed;
  }

  public List<ClinicalCareTask> evaluateOverdueAndEscalations(
      UUID caseId,
      ClinicalEncounter latestEncounter,
      ClinicalDecisionSupport decisionSupport,
      TreatmentResponse treatmentResponse,
      Instant currentTime) {

    Instant now = currentTime != null ? currentTime : Instant.now();
    List<ClinicalCareTask> activeTasks = taskRepository.findTasksByCaseId(caseId);
    List<FollowUpSchedule> schedules = taskRepository.findFollowUpSchedulesByCaseId(caseId);

    for (ClinicalCareTask task : activeTasks) {
      if (task.dueAt() != null && task.dueAt().isBefore(now) && task.status() == CareTaskStatus.PENDING) {
        taskRepository.updateTaskStatus(task.taskId(), CareTaskStatus.OVERDUE);

        if (eventPublisher != null) {
          eventPublisher.publishEvent(
              new ClinicalCareTaskOverdueEvent(caseId, task.taskId(), task.type(), task.priority(), Instant.now()));
        }
        if (metricsCollector != null) {
          metricsCollector.recordCareTaskOverdue(task.type().name(), task.priority().name());
        }
      }
    }

    List<ClinicalCareTask> escalatedTasks = escalationEngine.evaluateEscalations(
        activeTasks, schedules, latestEncounter, decisionSupport, treatmentResponse, now);

    for (ClinicalCareTask esc : escalatedTasks) {
      if (esc.status() == CareTaskStatus.ESCALATED) {
        taskRepository.updateTaskStatus(esc.taskId(), CareTaskStatus.ESCALATED);

        caseRepository.appendTimelineEvent(
            new ClinicalTimelineEvent(
                UUID.randomUUID(),
                caseId,
                now,
                ClinicalTimelineEventType.CARE_TASK_ESCALATED,
                "Care task escalated: " + esc.title(),
                esc.sourceEncounterId(),
                Map.of("taskId", esc.taskId().toString(), "priority", esc.priority().name())));

        if (eventPublisher != null) {
          eventPublisher.publishEvent(
              new ClinicalCareTaskEscalatedEvent(caseId, esc.taskId(), esc.type(), esc.priority(), "Care task escalation triggered", Instant.now()));
        }
        if (metricsCollector != null) {
          metricsCollector.recordCareTaskEscalated(esc.type().name(), esc.priority().name());
        }
      }
    }

    return taskRepository.findTasksByCaseId(caseId);
  }

  public CareTaskSummary getCareTaskSummary(UUID caseId) {
    List<ClinicalCareTask> tasks = taskRepository.findTasksByCaseId(caseId);
    int total = tasks.size();
    int pending = (int) tasks.stream().filter(t -> t.status() == CareTaskStatus.PENDING).count();
    int due = (int) tasks.stream().filter(t -> t.status() == CareTaskStatus.DUE).count();
    int overdue = (int) tasks.stream().filter(t -> t.status() == CareTaskStatus.OVERDUE).count();
    int completed = (int) tasks.stream().filter(t -> t.status() == CareTaskStatus.COMPLETED).count();
    int escalated = (int) tasks.stream().filter(t -> t.status() == CareTaskStatus.ESCALATED).count();
    int emergency = (int) tasks.stream().filter(t -> t.priority() == app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskPriority.EMERGENCY).count();
    int vetTasks = (int) tasks.stream().filter(t -> t.actor() == CareTaskActor.VETERINARIAN).count();

    Instant nextDueAt = tasks.stream()
        .filter(t -> t.dueAt() != null && t.status() != CareTaskStatus.COMPLETED && t.status() != CareTaskStatus.CANCELLED)
        .map(ClinicalCareTask::dueAt)
        .min(Instant::compareTo)
        .orElse(null);

    return new CareTaskSummary(
        caseId, total, pending, due, overdue, completed, escalated, emergency, vetTasks, nextDueAt);
  }

  private boolean isTaskDuplicate(ClinicalCareTask candidate, List<ClinicalCareTask> existingTasks) {
    String candActionId = candidate.sourceActionId() != null ? candidate.sourceActionId() : "NONE";
    String candFollowUpId = candidate.sourceFollowUpId() != null ? candidate.sourceFollowUpId() : "NONE";

    return existingTasks.stream().anyMatch(t -> {
      if (t.status() == CareTaskStatus.COMPLETED || t.status() == CareTaskStatus.CANCELLED) {
        return false;
      }
      String actId = t.sourceActionId() != null ? t.sourceActionId() : "NONE";
      String fUpId = t.sourceFollowUpId() != null ? t.sourceFollowUpId() : "NONE";
      return t.type() == candidate.type()
          && t.actor() == candidate.actor()
          && actId.equalsIgnoreCase(candActionId)
          && fUpId.equalsIgnoreCase(candFollowUpId);
    });
  }
}
