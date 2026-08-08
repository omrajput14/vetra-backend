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
import app.vetra.ai.workflow.clinical.evidence.ClinicalEvidenceAggregator;
import app.vetra.ai.workflow.clinical.model.ClinicalWorkflowContext;
import app.vetra.ai.workflow.clinical.model.ClinicalWorkflowRequest;
import app.vetra.ai.workflow.clinical.model.WorkflowStatus;
import app.vetra.ai.workflow.clinical.triage.ClinicalTriageEngine;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WorkflowStepTest {

  private AgentGateway agentGateway;
  private DiseaseRanker diseaseRanker;
  private ClinicalReportBuilder reportBuilder;
  private ClinicalEvidenceAggregator aggregator;
  private ClinicalTriageEngine triageEngine;

  private DiagnosisStep diagnosisStep;
  private EvidenceAggregationStep aggregationStep;
  private KnowledgeStep knowledgeStep;
  private RankingStep rankingStep;
  private ClinicalTriageStep triageStep;
  private TreatmentStep treatmentStep;
  private ReportStep reportStep;

  @BeforeEach
  void setUp() {
    agentGateway = mock(AgentGateway.class);
    diseaseRanker = new DiseaseRanker();
    reportBuilder = new ClinicalReportBuilder();
    aggregator = new ClinicalEvidenceAggregator();
    triageEngine = mock(ClinicalTriageEngine.class);

    diagnosisStep = new DiagnosisStep(agentGateway);
    aggregationStep = new EvidenceAggregationStep(aggregator, null, null);
    knowledgeStep = new KnowledgeStep(agentGateway);
    rankingStep = new RankingStep(diseaseRanker);
    triageStep = new ClinicalTriageStep(triageEngine, null);
    treatmentStep = new TreatmentStep(agentGateway);
    reportStep = new ReportStep(reportBuilder);
  }

  @Test
  void testStepOrdersAndNames() {
    assertEquals("diagnosis", diagnosisStep.stepName());
    assertEquals(1, diagnosisStep.order());

    assertEquals("evidence_aggregation", aggregationStep.stepName());
    assertEquals(2, aggregationStep.order());

    assertEquals("knowledge", knowledgeStep.stepName());
    assertEquals(3, knowledgeStep.order());

    assertEquals("ranking", rankingStep.stepName());
    assertEquals(4, rankingStep.order());

    assertEquals("triage", triageStep.stepName());
    assertEquals(5, triageStep.order());

    DecisionSupportStep decisionSupportStep = new DecisionSupportStep(new app.vetra.ai.workflow.clinical.explainability.ClinicalDecisionSupportEngine(), null, null);
    ActionPlanStep actionPlanStep = new ActionPlanStep(new app.vetra.ai.workflow.clinical.action.ClinicalActionPlanEngine(), null, null);

    assertEquals("treatment", treatmentStep.stepName());
    assertEquals(6, treatmentStep.order());

    assertEquals("decision_support", decisionSupportStep.stepName());
    assertEquals(7, decisionSupportStep.order());

    assertEquals("action_plan", actionPlanStep.stepName());
    assertEquals(8, actionPlanStep.order());

    assertEquals("report", reportStep.stepName());
    assertEquals(9, reportStep.order());
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
