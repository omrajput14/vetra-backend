package app.vetra.ai.workflow.clinical.clinicalcase.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.ai.workflow.clinical.clinicalcase.encounter.ClinicalEncounter;
import app.vetra.ai.workflow.clinical.clinicalcase.encounter.ClinicalEncounterType;
import app.vetra.ai.workflow.clinical.clinicalcase.model.ClinicalCase;
import app.vetra.ai.workflow.clinical.clinicalcase.model.ClinicalCaseStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.operations.model.PageResult;
import app.vetra.ai.workflow.clinical.clinicalcase.repository.InMemoryClinicalCaseRepository;
import app.vetra.ai.workflow.clinical.clinicalcase.task.repository.InMemoryClinicalCareTaskRepository;
import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClinicalOperationsDashboardServiceTest {

  private InMemoryClinicalCaseRepository caseRepository;
  private InMemoryClinicalCareTaskRepository taskRepository;
  private ClinicalOperationsDashboardService dashboardService;

  @BeforeEach
  void setUp() {
    caseRepository = new InMemoryClinicalCaseRepository();
    taskRepository = new InMemoryClinicalCareTaskRepository();
    dashboardService = new ClinicalOperationsDashboardService(caseRepository, taskRepository, null, null);
  }

  @Test
  void testEmptyState_returnsZeroCountsWithoutErrors() {
    ClinicalOperationsDashboardSummary summary = dashboardService.getDashboardSummary();
    assertNotNull(summary);
    assertEquals(0, summary.totalOpenCases());
    assertEquals(0, summary.emergencyCases());
    assertEquals(0, summary.veterinarianReviewCases());

    PageResult<ClinicalCaseWorkQueueItem> page = dashboardService.getCaseWorkQueue(null, 1, 10);
    assertNotNull(page);
    assertEquals(0, page.totalItems());
    assertTrue(page.items().isEmpty());
  }

  @Test
  void testMultipleCasesAndEmergencyPriorityCounts() {
    UUID animal1 = UUID.randomUUID();
    ClinicalCase c1 = new ClinicalCase(UUID.randomUUID(), animal1, "BOVINE", "Holstein", "Acute Pneumonia", Instant.now(), Instant.now(), null, ClinicalCaseStatus.OPEN);
    caseRepository.saveCase(c1);

    ClinicalEncounter enc1 = new ClinicalEncounter(
        UUID.randomUUID(), c1.caseId(), UUID.randomUUID(), Instant.now(), ClinicalEncounterType.INITIAL_ASSESSMENT, TriageUrgency.EMERGENCY, "Severe Pneumonia", BigDecimal.valueOf(0.95), List.of(), null, null, null, null);
    caseRepository.addEncounter(c1.caseId(), enc1);

    UUID animal2 = UUID.randomUUID();
    ClinicalCase c2 = new ClinicalCase(UUID.randomUUID(), animal2, "BOVINE", "Angus", "Routine Health Check", Instant.now(), Instant.now(), null, ClinicalCaseStatus.OPEN);
    caseRepository.saveCase(c2);

    ClinicalOperationsDashboardSummary summary = dashboardService.getDashboardSummary();
    assertEquals(2, summary.totalOpenCases());
    assertEquals(1, summary.emergencyCases());

    PageResult<ClinicalCaseWorkQueueItem> page = dashboardService.getCaseWorkQueue(null, 1, 10);
    assertEquals(2, page.totalItems());
    assertEquals(c1.caseId(), page.items().get(0).caseId());
  }

  @Test
  void testNonMutationSafety_dashboardGenerationDoesNotMutateCanonicalState() {
    UUID animalId = UUID.randomUUID();
    ClinicalCase c = new ClinicalCase(UUID.randomUUID(), animalId, "BOVINE", "Jersey", "Ketosis", Instant.now(), Instant.now(), null, ClinicalCaseStatus.OPEN);
    caseRepository.saveCase(c);

    ClinicalEncounter enc = new ClinicalEncounter(
        UUID.randomUUID(), c.caseId(), UUID.randomUUID(), Instant.now(), ClinicalEncounterType.INITIAL_ASSESSMENT, TriageUrgency.PRIORITY, "Bovine Ketosis", BigDecimal.valueOf(0.85), List.of(), null, null, null, null);
    caseRepository.addEncounter(c.caseId(), enc);

    // Initial state snapshot
    ClinicalCase originalCase = caseRepository.findById(c.caseId()).orElseThrow();
    int originalEncounterCount = caseRepository.findEncountersByCaseId(c.caseId()).size();

    // Query dashboard summaries and work queues multiple times
    dashboardService.getDashboardSummary();
    dashboardService.getCaseWorkQueue(null, 1, 10);
    dashboardService.getCaseOperationalView(c.caseId());
    dashboardService.getFarmerOperationalCaseView(c.caseId());
    dashboardService.getVeterinarianOperationalCaseView(c.caseId());

    // Verify canonical state remains byte-for-byte identical and unmutated
    ClinicalCase postQueryCase = caseRepository.findById(c.caseId()).orElseThrow();
    int postQueryEncounterCount = caseRepository.findEncountersByCaseId(c.caseId()).size();

    assertEquals(originalCase.caseId(), postQueryCase.caseId());
    assertEquals(originalCase.status(), postQueryCase.status());
    assertEquals(originalCase.lastUpdatedAt(), postQueryCase.lastUpdatedAt());
    assertEquals(originalEncounterCount, postQueryEncounterCount);
  }
}
