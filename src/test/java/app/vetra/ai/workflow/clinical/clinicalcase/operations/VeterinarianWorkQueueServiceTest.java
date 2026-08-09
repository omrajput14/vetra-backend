package app.vetra.ai.workflow.clinical.clinicalcase.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.vetra.ai.workflow.clinical.clinicalcase.encounter.ClinicalEncounter;
import app.vetra.ai.workflow.clinical.clinicalcase.encounter.ClinicalEncounterType;
import app.vetra.ai.workflow.clinical.clinicalcase.model.ClinicalCase;
import app.vetra.ai.workflow.clinical.clinicalcase.model.ClinicalCaseStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.operations.model.PageResult;
import app.vetra.ai.workflow.clinical.clinicalcase.repository.InMemoryClinicalCaseRepository;
import app.vetra.ai.workflow.clinical.clinicalcase.task.repository.InMemoryClinicalCareTaskRepository;
import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import app.vetra.ai.workflow.clinical.model.explainability.ClinicalDecisionSupport;
import app.vetra.ai.workflow.clinical.model.explainability.VeterinarianReviewFlag;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VeterinarianWorkQueueServiceTest {

  private InMemoryClinicalCaseRepository caseRepository;
  private InMemoryClinicalCareTaskRepository taskRepository;
  private ClinicalOperationsDashboardService dashboardService;
  private VeterinarianWorkQueueService vetQueueService;

  @BeforeEach
  void setUp() {
    caseRepository = new InMemoryClinicalCaseRepository();
    taskRepository = new InMemoryClinicalCareTaskRepository();
    dashboardService = new ClinicalOperationsDashboardService(caseRepository, taskRepository, null, null);
    vetQueueService = new VeterinarianWorkQueueService(dashboardService);
  }

  @Test
  void testGetEmergencyQueue_andVeterinarianReviewQueue() {
    UUID animal1 = UUID.randomUUID();
    ClinicalCase c1 = new ClinicalCase(UUID.randomUUID(), animal1, "BOVINE", "Holstein", "Severe Colic", Instant.now(), Instant.now(), null, ClinicalCaseStatus.OPEN);
    caseRepository.saveCase(c1);

    ClinicalEncounter enc1 = new ClinicalEncounter(
        UUID.randomUUID(), c1.caseId(), UUID.randomUUID(), Instant.now(), ClinicalEncounterType.INITIAL_ASSESSMENT, TriageUrgency.EMERGENCY, "Colic Emergency", BigDecimal.valueOf(0.98), List.of(), null, null, null, null);
    caseRepository.addEncounter(c1.caseId(), enc1);

    UUID animal2 = UUID.randomUUID();
    ClinicalCase c2 = new ClinicalCase(UUID.randomUUID(), animal2, "BOVINE", "Angus", "Ketosis", Instant.now(), Instant.now(), null, ClinicalCaseStatus.OPEN);
    caseRepository.saveCase(c2);

    VeterinarianReviewFlag flag = new VeterinarianReviewFlag(true, List.of("Requires Vet Review"), List.of());
    ClinicalDecisionSupport cds = new ClinicalDecisionSupport("Ketosis suspicion", List.of(), null, null, null, null, flag, Map.of(), Instant.now());
    caseRepository.saveDecisionSupport(c2.caseId(), cds);

    PageResult<ClinicalCaseWorkQueueItem> emergencyPage = vetQueueService.getEmergencyQueue(1, 10);
    assertNotNull(emergencyPage);
    assertEquals(1, emergencyPage.totalItems());
    assertEquals(c1.caseId(), emergencyPage.items().get(0).caseId());

    PageResult<ClinicalCaseWorkQueueItem> reviewPage = vetQueueService.getVeterinarianReviewQueue(1, 10);
    assertNotNull(reviewPage);
    assertEquals(1, reviewPage.totalItems());
    assertEquals(c2.caseId(), reviewPage.items().get(0).caseId());
  }
}
