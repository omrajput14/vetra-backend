package app.vetra.ai.workflow.clinical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.ai.rag.model.Citation;
import app.vetra.ai.rag.model.RetrievedContext;
import app.vetra.ai.workflow.clinical.model.ClinicalDiagnosisReport;
import app.vetra.ai.workflow.clinical.model.ClinicalWorkflowContext;
import app.vetra.ai.workflow.clinical.model.ClinicalWorkflowRequest;
import app.vetra.ai.workflow.clinical.model.DiseaseCandidate;
import app.vetra.ai.workflow.clinical.model.TreatmentPlan;
import app.vetra.ai.workflow.clinical.model.WorkflowStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClinicalReportBuilderTest {

  private ClinicalReportBuilder reportBuilder;

  @BeforeEach
  void setUp() {
    reportBuilder = new ClinicalReportBuilder();
  }

  @Test
  void testBuildReport_assemblesComprehensiveReport() {
    UUID scanId = UUID.randomUUID();
    UUID animalId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    ClinicalWorkflowRequest request =
        new ClinicalWorkflowRequest(
            scanId,
            animalId,
            "CATTLE",
            "Holstein",
            "https://cdn.vetra.app/scans/bovine_fmd.jpg",
            List.of("Mouth blisters", "Lameness", "High fever"),
            userId,
            null,
            null);

    ClinicalWorkflowContext context = new ClinicalWorkflowContext(request);

    // Setup intermediate state
    List<Citation> citations =
        List.of(new Citation("FAO Livestock Pathology Guide", "FAO-88", "FAO", 0.92));
    RetrievedContext retrievedContext =
        new RetrievedContext("Grounded literature content.", citations, 1, 20, 0.92);
    context.setRetrievedContext(retrievedContext);

    DiseaseCandidate topCandidate =
        new DiseaseCandidate(
            "Foot and Mouth Disease",
            new BigDecimal("0.95"),
            "Clinical vesicle lesions.",
            citations,
            true);
    context.setRankedDiseases(List.of(topCandidate));

    TreatmentPlan plan =
        new TreatmentPlan(
            "Supportive hydration and antiseptic mouth wash",
            List.of("Analgesic flunixin meglumine"),
            List.of("Isolate herd immediately"),
            List.of("Monitor appetite daily"),
            3);
    context.setTreatmentPlan(plan);
    context.setStatus(WorkflowStatus.SUCCESS);
    context.recordStepTiming("diagnosis", 150L);
    context.recordStepTiming("treatment", 120L);

    ClinicalDiagnosisReport report = reportBuilder.buildReport(context);

    assertNotNull(report);
    assertNotNull(report.reportId());
    assertEquals(scanId, report.scanId());
    assertEquals(animalId, report.animalId());
    assertEquals("Foot and Mouth Disease", report.primaryDiagnosis());
    assertEquals(0, BigDecimal.valueOf(0.95).compareTo(report.confidenceScore()));
    assertFalse(report.immediateActions().isEmpty());
    assertTrue(report.immediateActions().contains("Requires urgent veterinarian review"));
    assertEquals(1, report.references().size());
    assertEquals("FAO-88", report.references().get(0).chunkId());
    assertEquals(WorkflowStatus.SUCCESS, report.status());
    assertTrue(report.agentExecutionSummary().containsKey("stepTimings"));
  }

  @Test
  void testBuildReport_withEmptyContextProducesSafeDefaults() {
    ClinicalWorkflowRequest request =
        new ClinicalWorkflowRequest(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "SHEEP",
            "Merino",
            "https://cdn.vetra.app/scans/sheep.jpg",
            List.of(),
            UUID.randomUUID(),
            null,
            null);

    ClinicalWorkflowContext context = new ClinicalWorkflowContext(request);
    ClinicalDiagnosisReport report = reportBuilder.buildReport(context);

    assertNotNull(report);
    assertEquals("Unspecified Observation", report.primaryDiagnosis());
    assertEquals(0, BigDecimal.valueOf(0.10).compareTo(report.confidenceScore()));
  }
}
