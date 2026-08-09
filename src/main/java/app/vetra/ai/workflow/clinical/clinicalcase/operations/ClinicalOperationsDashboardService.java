package app.vetra.ai.workflow.clinical.clinicalcase.operations;

import app.vetra.ai.observability.AIMetricsCollector;
import app.vetra.ai.workflow.clinical.action.ClinicalActionPlanEngine;
import app.vetra.ai.workflow.clinical.clinicalcase.encounter.ClinicalEncounter;
import app.vetra.ai.workflow.clinical.clinicalcase.followup.ClinicalFollowUp;
import app.vetra.ai.workflow.clinical.clinicalcase.followup.FollowUpStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.model.ClinicalCase;
import app.vetra.ai.workflow.clinical.clinicalcase.model.ClinicalCaseStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.operations.model.PageResult;
import app.vetra.ai.workflow.clinical.clinicalcase.repository.ClinicalCaseRepository;
import app.vetra.ai.workflow.clinical.clinicalcase.response.TreatmentResponse;
import app.vetra.ai.workflow.clinical.clinicalcase.response.TreatmentResponseStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskActor;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskPriority;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskType;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.ClinicalCareTask;
import app.vetra.ai.workflow.clinical.clinicalcase.task.repository.ClinicalCareTaskRepository;
import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import app.vetra.ai.workflow.clinical.model.action.ClinicalActionPlan;
import app.vetra.ai.workflow.clinical.model.explainability.ClinicalDecisionSupport;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Pure deterministic projection service composing existing repository abstractions into operational read models,
 * work queues, role views, and dashboard summaries.
 */
@Service
public class ClinicalOperationsDashboardService {

  private static final Logger log = LoggerFactory.getLogger(ClinicalOperationsDashboardService.class);

  private final ClinicalCaseRepository caseRepository;
  private final ClinicalCareTaskRepository careTaskRepository;
  private final ClinicalActionPlanEngine actionPlanEngine;
  private final AIMetricsCollector metricsCollector;

  @Autowired
  public ClinicalOperationsDashboardService(
      ClinicalCaseRepository caseRepository,
      ClinicalCareTaskRepository careTaskRepository,
      ClinicalActionPlanEngine actionPlanEngine,
      AIMetricsCollector metricsCollector) {
    this.caseRepository = caseRepository;
    this.careTaskRepository = careTaskRepository;
    this.actionPlanEngine = actionPlanEngine;
    this.metricsCollector = metricsCollector;
  }

  /**
   * Constructs the aggregate operational dashboard summary.
   *
   * @return {@link ClinicalOperationsDashboardSummary} containing deterministic aggregate counts
   */
  public ClinicalOperationsDashboardSummary getDashboardSummary() {
    List<ClinicalCase> allCases = caseRepository.findAllCases();
    SummaryAccumulator acc = new SummaryAccumulator();

    for (ClinicalCase c : allCases) {
      accumulateCaseMetrics(c, acc);
    }

    List<ClinicalCareTask> activeTasks = loadActiveCareTasks();
    long overdueCareTasks = activeTasks.stream().filter(t -> t.status() == CareTaskStatus.OVERDUE).count();
    long pendingCareTasks = activeTasks.stream().filter(t -> t.status() == CareTaskStatus.PENDING).count();
    long escalatedCareTasks = activeTasks.stream().filter(t -> t.status() == CareTaskStatus.ESCALATED || t.escalationRequired()).count();

    if (metricsCollector != null) {
      metricsCollector.recordOperationsDashboard();
    }

    return new ClinicalOperationsDashboardSummary(
        acc.totalOpen, acc.emergencyCases, acc.vetReviewCases, acc.worseningCases,
        acc.activeTreatmentCases, acc.followUpRequiredCases, acc.overdueFollowUps,
        overdueCareTasks, pendingCareTasks, escalatedCareTasks,
        acc.referredCases, acc.resolvedCases, acc.closedCases,
        acc.earliestNextDue, acc.emergencyCases, overdueCareTasks + acc.overdueFollowUps,
        acc.vetReviewCases, Instant.now());
  }

  private void accumulateCaseMetrics(ClinicalCase c, SummaryAccumulator acc) {
    if (c.status() == ClinicalCaseStatus.OPEN || c.status() == ClinicalCaseStatus.UNDER_TREATMENT || c.status() == ClinicalCaseStatus.FOLLOW_UP_REQUIRED) {
      acc.totalOpen++;
    }
    updateStatusCounts(c.status(), acc);

    ClinicalCaseOperationalView view = getCaseOperationalView(c.caseId()).orElse(null);
    if (view != null) {
      accumulateOperationalView(view, acc);
    }
  }

  private void updateStatusCounts(ClinicalCaseStatus status, SummaryAccumulator acc) {
    if (status == ClinicalCaseStatus.UNDER_TREATMENT) {
      acc.activeTreatmentCases++;
    } else if (status == ClinicalCaseStatus.FOLLOW_UP_REQUIRED) {
      acc.followUpRequiredCases++;
    } else if (status == ClinicalCaseStatus.REFERRED) {
      acc.referredCases++;
    } else if (status == ClinicalCaseStatus.RESOLVED) {
      acc.resolvedCases++;
    } else if (status == ClinicalCaseStatus.CLOSED) {
      acc.closedCases++;
    }
  }

  private void accumulateOperationalView(ClinicalCaseOperationalView view, SummaryAccumulator acc) {
    if (view.emergency()) {
      acc.emergencyCases++;
    }
    if (view.veterinarianReviewRequired()) {
      acc.vetReviewCases++;
    }
    if (view.treatmentResponseStatus() == TreatmentResponseStatus.WORSENING) {
      acc.worseningCases++;
    }
    if (view.overdueFollowUpCount() > 0) {
      acc.overdueFollowUps++;
    }
    if (view.nextDueAt() != null && (acc.earliestNextDue == null || view.nextDueAt().isBefore(acc.earliestNextDue))) {
      acc.earliestNextDue = view.nextDueAt();
    }
  }

  private List<ClinicalCareTask> loadActiveCareTasks() {
    List<ClinicalCareTask> tasks = new ArrayList<>();
    CareTaskStatus[] activeStatuses = {
        CareTaskStatus.PENDING, CareTaskStatus.ASSIGNED, CareTaskStatus.IN_PROGRESS,
        CareTaskStatus.DUE, CareTaskStatus.OVERDUE, CareTaskStatus.ESCALATED
    };
    for (CareTaskStatus st : activeStatuses) {
      tasks.addAll(careTaskRepository.findTasksByStatus(st));
    }
    return tasks;
  }

  /**
   * Retrieves the case work queue filtered by queue reason with deterministic precedence ordering and pagination.
   *
   * @param filterReason optional filter reason
   * @param page 1-indexed page
   * @param pageSize items per page
   * @return paginated result of {@link ClinicalCaseWorkQueueItem}
   */
  public PageResult<ClinicalCaseWorkQueueItem> getCaseWorkQueue(CaseWorkQueueReason filterReason, int page, int pageSize) {
    List<ClinicalCase> allCases = caseRepository.findAllCases();
    List<ClinicalCaseWorkQueueItem> items = new ArrayList<>();

    for (ClinicalCase c : allCases) {
      if (c.status() == ClinicalCaseStatus.RESOLVED || c.status() == ClinicalCaseStatus.CLOSED) {
        continue;
      }
      ClinicalCaseOperationalView view = getCaseOperationalView(c.caseId()).orElse(null);
      if (view == null) {
        continue;
      }

      CaseWorkQueueReason reason = determineQueueReason(view);
      if (filterReason == null || reason == filterReason) {
        items.add(toWorkQueueItem(view, reason));
      }
    }

    items.sort(CasePrecedenceComparator.INSTANCE);

    if (metricsCollector != null) {
      metricsCollector.recordCaseQueueQuery(filterReason != null ? filterReason.name() : "ALL");
    }

    return PageResult.of(items, page, pageSize);
  }

  private ClinicalCaseWorkQueueItem toWorkQueueItem(ClinicalCaseOperationalView view, CaseWorkQueueReason reason) {
    CareTaskPriority priority = determineQueuePriority(view);
    return new ClinicalCaseWorkQueueItem(
        view.caseId(), view.animalId(), view.operationalStatus(), priority, view.latestUrgency(),
        view.veterinarianReviewRequired(), view.treatmentResponseStatus(), view.openTaskCount(),
        view.overdueTaskCount(), view.nextDueAt(), view.latestEncounterAt(), reason, view.nextOperationalAction());
  }

  /**
   * Builds a complete {@link ClinicalCaseOperationalView} for a given case ID.
   *
   * @param caseId target case ID
   * @return optional operational view projection
   */
  public Optional<ClinicalCaseOperationalView> getCaseOperationalView(UUID caseId) {
    Optional<ClinicalCase> cOpt = caseRepository.findById(caseId);
    if (cOpt.isEmpty()) {
      return Optional.empty();
    }
    ClinicalCase c = cOpt.get();

    List<ClinicalEncounter> encounters = caseRepository.findEncountersByCaseId(caseId);
    ClinicalEncounter latestEnc = encounters.isEmpty() ? null : encounters.get(encounters.size() - 1);

    ClinicalDecisionSupport cds = caseRepository.findLatestDecisionSupportByCaseId(caseId).orElse(null);
    ClinicalActionPlan plan = caseRepository.findLatestActionPlanByCaseId(caseId).orElse(null);
    TreatmentResponse response = caseRepository.findLatestTreatmentResponseByCaseId(caseId).orElse(null);
    List<ClinicalFollowUp> followUps = caseRepository.findFollowUpsByCaseId(caseId);
    List<ClinicalCareTask> careTasks = careTaskRepository.findTasksByCaseId(caseId);

    return Optional.of(buildOperationalView(c, latestEnc, cds, plan, response, followUps, careTasks));
  }

  private ClinicalCaseOperationalView buildOperationalView(
      ClinicalCase c,
      ClinicalEncounter latestEnc,
      ClinicalDecisionSupport cds,
      ClinicalActionPlan plan,
      TreatmentResponse response,
      List<ClinicalFollowUp> followUps,
      List<ClinicalCareTask> careTasks) {

    boolean emergency = isEmergencyCase(latestEnc, response);
    boolean vetReviewRequired = isVetReviewRequired(cds, plan);

    Instant now = Instant.now();
    TaskCounts taskCounts = countTasks(careTasks, now);
    FollowUpCounts fuCounts = countFollowUps(followUps);

    CaseOperationalStatus opStatus = determineOperationalStatus(c, response, vetReviewRequired, emergency, taskCounts.overdue, fuCounts.overdue, fuCounts.pending);
    Instant nextDueAt = findNextDueAt(careTasks, followUps, now);
    String nextAction = determineNextActionTitle(careTasks, plan, response, opStatus);

    return new ClinicalCaseOperationalView(
        c.caseId(), c.animalId(), c.species(), c.breed(), c.primaryCondition(), c.status(), opStatus,
        latestEnc != null ? latestEnc.encounterId() : null,
        latestEnc != null ? latestEnc.occurredAt() : c.openedAt(),
        latestEnc != null ? latestEnc.urgency() : TriageUrgency.ROUTINE,
        latestEnc != null ? latestEnc.primaryDiagnosis() : c.primaryCondition(),
        latestEnc != null ? latestEnc.diagnosticConfidence() : null,
        response != null ? response.status() : TreatmentResponseStatus.INSUFFICIENT_DATA,
        vetReviewRequired, emergency, taskCounts.open, taskCounts.overdue, taskCounts.emergency,
        fuCounts.pending, fuCounts.overdue, nextDueAt, nextAction, c.lastUpdatedAt());
  }

  private boolean isEmergencyCase(ClinicalEncounter enc, TreatmentResponse response) {
    return (enc != null && enc.urgency() == TriageUrgency.EMERGENCY)
        || (response != null && response.status() == TreatmentResponseStatus.WORSENING && enc != null && enc.urgency() == TriageUrgency.URGENT);
  }

  private boolean isVetReviewRequired(ClinicalDecisionSupport cds, ClinicalActionPlan plan) {
    return (cds != null && cds.veterinarianReviewFlag() != null && cds.veterinarianReviewFlag().requiresReview())
        || (plan != null && plan.veterinarianReviewRequired());
  }

  private TaskCounts countTasks(List<ClinicalCareTask> tasks, Instant now) {
    int open = (int) tasks.stream().filter(t -> t.status() != CareTaskStatus.COMPLETED && t.status() != CareTaskStatus.CANCELLED).count();
    int overdue = (int) tasks.stream().filter(t -> t.status() == CareTaskStatus.OVERDUE || (t.dueAt() != null && t.dueAt().isBefore(now) && t.status() != CareTaskStatus.COMPLETED && t.status() != CareTaskStatus.CANCELLED)).count();
    int emergency = (int) tasks.stream().filter(t -> t.priority() == CareTaskPriority.EMERGENCY || t.escalationRequired()).count();
    return new TaskCounts(open, overdue, emergency);
  }

  private FollowUpCounts countFollowUps(List<ClinicalFollowUp> followUps) {
    int pending = (int) followUps.stream().filter(f -> f.status() == FollowUpStatus.SCHEDULED || f.status() == FollowUpStatus.DUE).count();
    int overdue = (int) followUps.stream().filter(f -> f.status() == FollowUpStatus.MISSED || f.status() == FollowUpStatus.ESCALATED).count();
    return new FollowUpCounts(pending, overdue);
  }

  /**
   * Constructs a data-minimized {@link FarmerOperationalCaseView} for caregivers.
   *
   * @param caseId target case ID
   * @return optional farmer operational case view
   */
  public Optional<FarmerOperationalCaseView> getFarmerOperationalCaseView(UUID caseId) {
    Optional<ClinicalCaseOperationalView> opViewOpt = getCaseOperationalView(caseId);
    if (opViewOpt.isEmpty()) {
      return Optional.empty();
    }
    ClinicalCaseOperationalView opView = opViewOpt.get();

    List<ClinicalCareTask> careTasks = careTaskRepository.findTasksByCaseId(caseId);
    List<ClinicalCareTask> farmerImmediateTasks = careTasks.stream()
        .filter(t -> t.actor() == CareTaskActor.CAREGIVER && (t.priority() == CareTaskPriority.EMERGENCY || t.priority() == CareTaskPriority.HIGH)).toList();
    List<ClinicalCareTask> farmerMonitoringTasks = careTasks.stream()
        .filter(t -> t.actor() == CareTaskActor.CAREGIVER && t.type() == CareTaskType.MONITORING).toList();

    String followUpDueStatus = opView.overdueFollowUpCount() > 0 ? "OVERDUE" : (opView.pendingFollowUpCount() > 0 ? "SCHEDULED" : "NONE");

    FarmerOperationalCaseView farmerView = new FarmerOperationalCaseView(
        opView.caseId(), opView.animalId(), opView.species(), opView.breed(), opView.primaryCondition(),
        opView.currentCaseStatus(), opView.veterinarianReviewRequired(), farmerImmediateTasks,
        farmerMonitoringTasks, followUpDueStatus, opView.emergency() || opView.emergencyTaskCount() > 0,
        opView.nextOperationalAction(), opView.lastUpdatedAt());

    return Optional.of(farmerView);
  }

  /**
   * Constructs a complete {@link VeterinarianOperationalCaseView} for clinical staff.
   *
   * @param caseId target case ID
   * @return optional veterinarian operational case view
   */
  public Optional<VeterinarianOperationalCaseView> getVeterinarianOperationalCaseView(UUID caseId) {
    Optional<ClinicalCaseOperationalView> opViewOpt = getCaseOperationalView(caseId);
    if (opViewOpt.isEmpty()) {
      return Optional.empty();
    }
    ClinicalCaseOperationalView opView = opViewOpt.get();

    List<ClinicalEncounter> encounters = caseRepository.findEncountersByCaseId(caseId);
    ClinicalEncounter latestEnc = encounters.isEmpty() ? null : encounters.get(encounters.size() - 1);
    ClinicalDecisionSupport cds = caseRepository.findLatestDecisionSupportByCaseId(caseId).orElse(null);
    TreatmentResponse response = caseRepository.findLatestTreatmentResponseByCaseId(caseId).orElse(null);

    List<ClinicalCareTask> careTasks = careTaskRepository.findTasksByCaseId(caseId);
    List<ClinicalCareTask> activeTasks = careTasks.stream().filter(t -> t.status() != CareTaskStatus.COMPLETED && t.status() != CareTaskStatus.CANCELLED).toList();
    List<ClinicalCareTask> overdueTasks = careTasks.stream().filter(t -> t.status() == CareTaskStatus.OVERDUE).toList();

    boolean hasConflicts = cds != null && cds.contradictoryEvidenceSummary() != null
        && (!cds.contradictoryEvidenceSummary().conflictingMeasurements().isEmpty() || !cds.contradictoryEvidenceSummary().diagnosticContradictions().isEmpty());

    String uncertainty = cds != null && cds.uncertaintyAssessment() != null && cds.uncertaintyAssessment().overallLevel() != null
        ? cds.uncertaintyAssessment().overallLevel().name() : "LOW";

    app.vetra.ai.workflow.clinical.clinicalcase.timeline.ClinicalCaseTimeline timeline = caseRepository.getTimeline(caseId);

    VeterinarianOperationalCaseView vetView = new VeterinarianOperationalCaseView(
        opView.caseId(), opView.animalId(), opView.species(), opView.breed(), opView.primaryCondition(),
        opView.currentCaseStatus(), opView.operationalStatus(), latestEnc, opView.latestDiagnosis(),
        opView.latestDiagnosticConfidence(), opView.latestUrgency(), response,
        cds != null ? cds.veterinarianReviewFlag() : null, hasConflicts, uncertainty, cds,
        activeTasks, overdueTasks, timeline, opView.nextOperationalAction(), opView.lastUpdatedAt());

    return Optional.of(vetView);
  }

  private CaseOperationalStatus determineOperationalStatus(
      ClinicalCase c, TreatmentResponse response,
      boolean vetReviewReq, boolean emergency, int overdueTasks, int overdueFollowUps, int pendingFollowUps) {

    if (emergency) {
      return CaseOperationalStatus.EMERGENCY;
    }
    if (vetReviewReq) {
      return CaseOperationalStatus.VETERINARIAN_REVIEW_REQUIRED;
    }
    if (response != null && response.status() == TreatmentResponseStatus.WORSENING) {
      return CaseOperationalStatus.WORSENING;
    }
    if (overdueFollowUps > 0) {
      return CaseOperationalStatus.FOLLOW_UP_OVERDUE;
    }
    if (overdueTasks > 0) {
      return CaseOperationalStatus.CARE_TASK_OVERDUE;
    }
    return determineSecondaryOperationalStatus(c, pendingFollowUps);
  }

  private CaseOperationalStatus determineSecondaryOperationalStatus(ClinicalCase c, int pendingFollowUps) {
    if (c.status() == ClinicalCaseStatus.UNDER_TREATMENT) {
      return CaseOperationalStatus.UNDER_TREATMENT;
    }
    if (pendingFollowUps > 0 || c.status() == ClinicalCaseStatus.FOLLOW_UP_REQUIRED) {
      return CaseOperationalStatus.FOLLOW_UP_REQUIRED;
    }
    if (c.status() == ClinicalCaseStatus.REFERRED) {
      return CaseOperationalStatus.REFERRED;
    }
    if (c.status() == ClinicalCaseStatus.RESOLVED) {
      return CaseOperationalStatus.RESOLVED;
    }
    if (c.status() == ClinicalCaseStatus.CLOSED) {
      return CaseOperationalStatus.CLOSED;
    }
    return CaseOperationalStatus.STABLE;
  }

  private CaseWorkQueueReason determineQueueReason(ClinicalCaseOperationalView view) {
    return switch (view.operationalStatus()) {
      case EMERGENCY -> CaseWorkQueueReason.EMERGENCY;
      case VETERINARIAN_REVIEW_REQUIRED -> CaseWorkQueueReason.VETERINARIAN_REVIEW;
      case WORSENING -> CaseWorkQueueReason.WORSENING_RESPONSE;
      case CARE_TASK_OVERDUE -> CaseWorkQueueReason.OVERDUE_CARE_TASK;
      case FOLLOW_UP_OVERDUE -> CaseWorkQueueReason.OVERDUE_FOLLOW_UP;
      case FOLLOW_UP_REQUIRED -> CaseWorkQueueReason.DUE_FOLLOW_UP;
      case UNDER_TREATMENT -> CaseWorkQueueReason.ACTIVE_TREATMENT;
      default -> CaseWorkQueueReason.ROUTINE_CASE_REVIEW;
    };
  }

  private CareTaskPriority determineQueuePriority(ClinicalCaseOperationalView view) {
    if (view.emergency()) {
      return CareTaskPriority.EMERGENCY;
    }
    if (view.veterinarianReviewRequired() || view.treatmentResponseStatus() == TreatmentResponseStatus.WORSENING || view.overdueTaskCount() > 0 || view.overdueFollowUpCount() > 0) {
      return CareTaskPriority.HIGH;
    }
    if (view.openTaskCount() > 0 || view.pendingFollowUpCount() > 0) {
      return CareTaskPriority.MEDIUM;
    }
    return CareTaskPriority.LOW;
  }

  private Instant findNextDueAt(List<ClinicalCareTask> tasks, List<ClinicalFollowUp> followUps, Instant now) {
    Instant earliest = null;
    for (ClinicalCareTask t : tasks) {
      if (t.status() != CareTaskStatus.COMPLETED && t.status() != CareTaskStatus.CANCELLED && t.dueAt() != null) {
        if (earliest == null || t.dueAt().isBefore(earliest)) {
          earliest = t.dueAt();
        }
      }
    }
    for (ClinicalFollowUp f : followUps) {
      if ((f.status() == FollowUpStatus.SCHEDULED || f.status() == FollowUpStatus.DUE) && f.scheduledAt() != null) {
        if (earliest == null || f.scheduledAt().isBefore(earliest)) {
          earliest = f.scheduledAt();
        }
      }
    }
    return earliest;
  }

  private String determineNextActionTitle(List<ClinicalCareTask> tasks, ClinicalActionPlan plan, TreatmentResponse response, CaseOperationalStatus status) {
    for (ClinicalCareTask t : tasks) {
      if (t.priority() == CareTaskPriority.EMERGENCY || t.status() == CareTaskStatus.OVERDUE) {
        return t.title();
      }
    }
    if (!tasks.isEmpty()) {
      return tasks.get(0).title();
    }
    if (plan != null && plan.immediateActions() != null && !plan.immediateActions().isEmpty()) {
      return plan.immediateActions().get(0).title();
    }
    return "Case Monitoring & Observation";
  }

  private static class SummaryAccumulator {
    long totalOpen = 0;
    long emergencyCases = 0;
    long vetReviewCases = 0;
    long worseningCases = 0;
    long activeTreatmentCases = 0;
    long followUpRequiredCases = 0;
    long overdueFollowUps = 0;
    long referredCases = 0;
    long resolvedCases = 0;
    long closedCases = 0;
    Instant earliestNextDue = null;
  }

  private record TaskCounts(int open, int overdue, int emergency) {}

  private record FollowUpCounts(int pending, int overdue) {}
}
