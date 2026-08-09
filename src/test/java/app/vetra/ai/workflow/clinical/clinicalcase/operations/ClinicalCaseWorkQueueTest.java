package app.vetra.ai.workflow.clinical.clinicalcase.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskPriority;
import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClinicalCaseWorkQueueTest {

  @Test
  void testCasePrecedenceComparator_emergencyFirstAndStableOrdering() {
    UUID case1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    UUID case2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    UUID case3 = UUID.fromString("00000000-0000-0000-0000-000000000003");

    Instant now = Instant.now();

    ClinicalCaseWorkQueueItem itemRoutine = new ClinicalCaseWorkQueueItem(
        case1, UUID.randomUUID(), CaseOperationalStatus.STABLE, CareTaskPriority.LOW, TriageUrgency.ROUTINE, false, null, 1, 0, now.plusSeconds(86400), now, CaseWorkQueueReason.ROUTINE_CASE_REVIEW, "Routine check");

    ClinicalCaseWorkQueueItem itemReview = new ClinicalCaseWorkQueueItem(
        case2, UUID.randomUUID(), CaseOperationalStatus.VETERINARIAN_REVIEW_REQUIRED, CareTaskPriority.HIGH, TriageUrgency.URGENT, true, null, 2, 0, now.plusSeconds(3600), now, CaseWorkQueueReason.VETERINARIAN_REVIEW, "Vet review");

    ClinicalCaseWorkQueueItem itemEmergency = new ClinicalCaseWorkQueueItem(
        case3, UUID.randomUUID(), CaseOperationalStatus.EMERGENCY, CareTaskPriority.EMERGENCY, TriageUrgency.EMERGENCY, true, null, 3, 1, now, now, CaseWorkQueueReason.EMERGENCY, "Emergency care");

    List<ClinicalCaseWorkQueueItem> list = new ArrayList<>(List.of(itemRoutine, itemReview, itemEmergency));
    list.sort(CasePrecedenceComparator.INSTANCE);

    assertEquals(case3, list.get(0).caseId());
    assertEquals(CaseOperationalStatus.EMERGENCY, list.get(0).operationalStatus());

    assertEquals(case2, list.get(1).caseId());
    assertEquals(CaseOperationalStatus.VETERINARIAN_REVIEW_REQUIRED, list.get(1).operationalStatus());

    assertEquals(case1, list.get(2).caseId());
    assertEquals(CaseOperationalStatus.STABLE, list.get(2).operationalStatus());
  }

  @Test
  void testEqualPriorityDeterministicTieBreakers() {
    UUID caseA = UUID.fromString("00000000-0000-0000-0000-000000000001");
    UUID caseB = UUID.fromString("00000000-0000-0000-0000-000000000002");

    Instant now = Instant.now();

    ClinicalCaseWorkQueueItem itemA = new ClinicalCaseWorkQueueItem(
        caseA, UUID.randomUUID(), CaseOperationalStatus.CARE_TASK_OVERDUE, CareTaskPriority.HIGH, TriageUrgency.URGENT, false, null, 1, 1, now.plusSeconds(100), now, CaseWorkQueueReason.OVERDUE_CARE_TASK, "Task A");

    ClinicalCaseWorkQueueItem itemB = new ClinicalCaseWorkQueueItem(
        caseB, UUID.randomUUID(), CaseOperationalStatus.CARE_TASK_OVERDUE, CareTaskPriority.HIGH, TriageUrgency.URGENT, false, null, 1, 1, now.plusSeconds(200), now, CaseWorkQueueReason.OVERDUE_CARE_TASK, "Task B");

    List<ClinicalCaseWorkQueueItem> list = new ArrayList<>(List.of(itemB, itemA));
    list.sort(CasePrecedenceComparator.INSTANCE);

    assertEquals(caseA, list.get(0).caseId());
    assertEquals(caseB, list.get(1).caseId());
  }
}
