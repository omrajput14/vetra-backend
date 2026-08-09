package app.vetra.ai.workflow.clinical.clinicalcase.task.repository;

import app.vetra.ai.workflow.clinical.clinicalcase.coordination.FollowUpSchedule;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskAssignment;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.ClinicalCareTask;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repository interface for operational clinical care tasks and assignments. */
public interface ClinicalCareTaskRepository {

  ClinicalCareTask createTask(ClinicalCareTask task);

  Optional<ClinicalCareTask> findTaskById(UUID taskId);

  List<ClinicalCareTask> findTasksByCaseId(UUID caseId);

  List<ClinicalCareTask> findTasksByStatus(CareTaskStatus status);

  List<ClinicalCareTask> findTasksDueBefore(Instant timestamp);

  List<ClinicalCareTask> findOverdueTasks(Instant currentTimestamp);

  ClinicalCareTask updateTaskStatus(UUID taskId, CareTaskStatus newStatus);

  CareTaskAssignment assignTask(CareTaskAssignment assignment);

  List<CareTaskAssignment> findAssignmentsByTaskId(UUID taskId);

  FollowUpSchedule saveFollowUpSchedule(FollowUpSchedule schedule);

  List<FollowUpSchedule> findFollowUpSchedulesByCaseId(UUID caseId);
}
