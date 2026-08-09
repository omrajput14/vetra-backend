package app.vetra.ai.workflow.clinical.clinicalcase.operations;

import app.vetra.ai.workflow.clinical.clinicalcase.operations.model.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Pure deterministic service providing specialized operational work queues for veterinarians.
 */
@Service
public class VeterinarianWorkQueueService {

  private static final Logger log = LoggerFactory.getLogger(VeterinarianWorkQueueService.class);
  private final ClinicalOperationsDashboardService dashboardService;

  public VeterinarianWorkQueueService(ClinicalOperationsDashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  /** Retrieves emergency cases work queue. */
  public PageResult<ClinicalCaseWorkQueueItem> getEmergencyQueue(int page, int pageSize) {
    return dashboardService.getCaseWorkQueue(CaseWorkQueueReason.EMERGENCY, page, pageSize);
  }

  /** Retrieves veterinarian review cases work queue. */
  public PageResult<ClinicalCaseWorkQueueItem> getVeterinarianReviewQueue(int page, int pageSize) {
    return dashboardService.getCaseWorkQueue(CaseWorkQueueReason.VETERINARIAN_REVIEW, page, pageSize);
  }

  /** Retrieves worsening treatment response cases work queue. */
  public PageResult<ClinicalCaseWorkQueueItem> getWorseningQueue(int page, int pageSize) {
    return dashboardService.getCaseWorkQueue(CaseWorkQueueReason.WORSENING_RESPONSE, page, pageSize);
  }

  /** Retrieves overdue care tasks work queue. */
  public PageResult<ClinicalCaseWorkQueueItem> getOverdueCareTaskQueue(int page, int pageSize) {
    return dashboardService.getCaseWorkQueue(CaseWorkQueueReason.OVERDUE_CARE_TASK, page, pageSize);
  }

  /** Retrieves overdue follow-ups work queue. */
  public PageResult<ClinicalCaseWorkQueueItem> getOverdueFollowUpQueue(int page, int pageSize) {
    return dashboardService.getCaseWorkQueue(CaseWorkQueueReason.OVERDUE_FOLLOW_UP, page, pageSize);
  }

  /** Retrieves routine case review work queue. */
  public PageResult<ClinicalCaseWorkQueueItem> getRoutineReviewQueue(int page, int pageSize) {
    return dashboardService.getCaseWorkQueue(CaseWorkQueueReason.ROUTINE_CASE_REVIEW, page, pageSize);
  }

  /** Retrieves master prioritized veterinarian work queue across all reasons. */
  public PageResult<ClinicalCaseWorkQueueItem> getMasterVeterinarianQueue(int page, int pageSize) {
    return dashboardService.getCaseWorkQueue(null, page, pageSize);
  }
}
