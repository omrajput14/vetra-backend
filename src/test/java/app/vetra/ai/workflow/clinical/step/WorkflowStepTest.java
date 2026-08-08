package app.vetra.ai.workflow.clinical.step;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.vetra.ai.agent.gateway.AgentGateway;
import app.vetra.ai.agent.model.AgentCapability;
import app.vetra.ai.agent.model.AgentResponse;
import app.vetra.ai.model.AIResponse;
import app.vetra.ai.workflow.clinical.ClinicalReportBuilder;
import app.vetra.ai.workflow.clinical.DiseaseRanker;
import app.vetra.ai.workflow.clinical.model.ClinicalWorkflowContext;
import app.vetra.ai.workflow.clinical.model.ClinicalWorkflowRequest;
import app.vetra.ai.workflow.clinical.model.WorkflowStatus;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WorkflowStepTest {

  private AgentGateway agentGateway;
  private DiseaseRanker diseaseRanker;
  private ClinicalReportBuilder reportBuilder;

  private DiagnosisStep diagnosisStep;
  private KnowledgeStep knowledgeStep;
  private RankingStep rankingStep;
  private TreatmentStep treatmentStep;
  private ReportStep reportStep;

  @BeforeEach
  void setUp() {
    agentGateway = mock(AgentGateway.class);
    diseaseRanker = new DiseaseRanker();
    reportBuilder = new ClinicalReportBuilder();

    diagnosisStep = new DiagnosisStep(agentGateway);
    knowledgeStep = new KnowledgeStep(agentGateway);
    rankingStep = new RankingStep(diseaseRanker);
    treatmentStep = new TreatmentStep(agentGateway);
    reportStep = new ReportStep(reportBuilder);
  }

  @Test
  void testStepOrdersAndNames() {
    assertEquals("diagnosis", diagnosisStep.stepName());
    assertEquals(1, diagnosisStep.order());

    assertEquals("knowledge", knowledgeStep.stepName());
    assertEquals(2, knowledgeStep.order());

    assertEquals("ranking", rankingStep.stepName());
    assertEquals(3, rankingStep.order());

    assertEquals("treatment", treatmentStep.stepName());
    assertEquals(5, treatmentStep.order());

    assertEquals("report", reportStep.stepName());
    assertEquals(6, reportStep.order());
  }

  @Test
  void testDiagnosisStep_execution() throws Exception {
    ClinicalWorkflowRequest request =
        new ClinicalWorkflowRequest(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "CATTLE",
            "Jersey",
            "https://cdn.vetra.app/img.jpg",
            List.of("Lethargy"),
            UUID.randomUUID(),
            null,
            null);

    ClinicalWorkflowContext context = new ClinicalWorkflowContext(request);

    AIResponse raw = new AIResponse("{\"condition\":\"Bovine Mastitis\",\"confidence\":0.88}", "1.0", "gemini", "gemini-1.5-pro", 30, 70, "STOP");
    AgentResponse resp = new AgentResponse(raw, "DiagnosisAgent", AgentCapability.DIAGNOSIS, Map.of());
    when(agentGateway.execute(any())).thenReturn(resp);

    diagnosisStep.execute(context);

    assertNotNull(context.getDiagnosisResponse());
    assertEquals(WorkflowStatus.SUCCESS, context.getStepStatuses().get("diagnosis"));
    verify(agentGateway).execute(any());
  }
}
