package app.vetra.ai.workflow.clinical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.vetra.ai.agent.gateway.AgentGateway;
import app.vetra.ai.agent.model.AgentCapability;
import app.vetra.ai.agent.model.AgentRequest;
import app.vetra.ai.agent.model.AgentResponse;
import app.vetra.ai.event.ClinicalWorkflowCompletedEvent;
import app.vetra.ai.event.ClinicalWorkflowStartedEvent;
import app.vetra.ai.model.AIResponse;
import app.vetra.ai.observability.AIMetricsCollector;
import app.vetra.ai.observability.AIObservationConvention;
import app.vetra.ai.workflow.clinical.evidence.ClinicalEvidenceAggregator;
import app.vetra.ai.workflow.clinical.explainability.ClinicalDecisionSupportEngine;
import app.vetra.ai.workflow.clinical.model.ClinicalWorkflowRequest;
import app.vetra.ai.workflow.clinical.model.ClinicalWorkflowResult;
import app.vetra.ai.workflow.clinical.model.WorkflowStatus;
import app.vetra.ai.workflow.clinical.step.ClinicalTriageStep;
import app.vetra.ai.workflow.clinical.step.DiagnosisStep;
import app.vetra.ai.workflow.clinical.step.EvidenceAggregationStep;
import app.vetra.ai.workflow.clinical.step.KnowledgeStep;
import app.vetra.ai.workflow.clinical.step.RankingStep;
import app.vetra.ai.workflow.clinical.step.ReportStep;
import app.vetra.ai.workflow.clinical.step.TreatmentStep;
import app.vetra.ai.workflow.clinical.triage.ClinicalTriageEngine;
import app.vetra.ai.workflow.clinical.triage.ClinicalTriageRules;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class ClinicalWorkflowEngineTest {

  private AgentGateway agentGateway;
  private AIMetricsCollector metricsCollector;
  private AIObservationConvention observationConvention;
  private ApplicationEventPublisher eventPublisher;

  private ClinicalWorkflowEngine workflowEngine;

  @BeforeEach
  void setUp() {
    agentGateway = mock(AgentGateway.class);
    metricsCollector = mock(AIMetricsCollector.class);
    observationConvention = mock(AIObservationConvention.class);
    eventPublisher = mock(ApplicationEventPublisher.class);

    DiseaseRanker diseaseRanker = new DiseaseRanker();
    ClinicalReportBuilder reportBuilder = new ClinicalReportBuilder();
    ClinicalTriageRules triageRules = new ClinicalTriageRules();
    ClinicalTriageEngine triageEngine = new ClinicalTriageEngine(triageRules, agentGateway);
    ClinicalEvidenceAggregator aggregator = new ClinicalEvidenceAggregator();

    DiagnosisStep diagnosisStep = new DiagnosisStep(agentGateway);
    EvidenceAggregationStep aggregationStep = new EvidenceAggregationStep(aggregator, eventPublisher, metricsCollector);
    KnowledgeStep knowledgeStep = new KnowledgeStep(agentGateway);
    RankingStep rankingStep = new RankingStep(diseaseRanker);
    ClinicalTriageStep triageStep = new ClinicalTriageStep(triageEngine, eventPublisher);
    TreatmentStep treatmentStep = new TreatmentStep(agentGateway);
    ClinicalDecisionSupportEngine cdsEngine = new ClinicalDecisionSupportEngine();
    app.vetra.ai.workflow.clinical.step.DecisionSupportStep decisionSupportStep =
        new app.vetra.ai.workflow.clinical.step.DecisionSupportStep(cdsEngine, eventPublisher, metricsCollector);
    app.vetra.ai.workflow.clinical.action.ClinicalActionPlanEngine actionPlanEngine = new app.vetra.ai.workflow.clinical.action.ClinicalActionPlanEngine();
    app.vetra.ai.workflow.clinical.step.ActionPlanStep actionPlanStep =
        new app.vetra.ai.workflow.clinical.step.ActionPlanStep(actionPlanEngine, eventPublisher, metricsCollector);
    ReportStep reportStep = new ReportStep(reportBuilder);

    workflowEngine =
        new ClinicalWorkflowEngine(
            List.of(diagnosisStep, aggregationStep, knowledgeStep, rankingStep, triageStep, treatmentStep, decisionSupportStep, actionPlanStep, reportStep),
            metricsCollector,
            observationConvention,
            eventPublisher);
  }

  @Test
  void testExecuteWorkflow_endToEndSuccess() {
    UUID scanId = UUID.randomUUID();
    UUID animalId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    ClinicalWorkflowRequest request =
        new ClinicalWorkflowRequest(
            scanId,
            animalId,
            "BOVINE",
            "Holstein",
            "https://cdn.vetra.app/img.jpg",
            List.of("Mouth lesions", "High fever"),
            userId,
            null,
            null);

    // Mock Diagnosis response
    String diagJson = "{\"condition\":\"Bovine Foot and Mouth Disease\",\"confidence\":0.93,\"requiresVeterinarianReview\":true}";
    AIResponse diagRaw = new AIResponse(diagJson, "1.0", "gemini", "gemini-1.5-pro", 40, 80, "STOP");
    AgentResponse diagResp = new AgentResponse(diagRaw, "DiagnosisAgent", AgentCapability.DIAGNOSIS, Map.of());

    // Mock Knowledge response
    String knowText = "Foot and mouth disease is caused by an aphthovirus.";
    AIResponse knowRaw = new AIResponse(knowText, "1.0", "gemini", "gemini-1.5-pro", 50, 90, "STOP");
    AgentResponse knowResp = new AgentResponse(knowRaw, "KnowledgeAgent", AgentCapability.KNOWLEDGE, Map.of("avgSimilarity", "0.91"));

    // Mock Triage response
    String triageJson = "{\"urgency\":\"URGENT\",\"confidence\":0.90,\"rationale\":\"FMD risk\",\"warningSigns\":[\"Fever\"],\"recommendedActions\":[\"Isolate\"],\"requiresImmediateVeterinaryReview\":true}";
    AIResponse triageRaw = new AIResponse(triageJson, "1.0", "gemini", "gemini-1.5-pro", 30, 70, "STOP");
    AgentResponse triageResp = new AgentResponse(triageRaw, "TriageAgent", AgentCapability.TRIAGE, Map.of());

    // Mock Treatment response
    String treatJson = "{\"treatmentPlan\":\"Isolate herd and supportive wash.\",\"prescriptions\":[\"Flunixin\"],\"precautions\":[\"Quarantine\"],\"monitoring\":[\"Feed intake\"],\"followUpDays\":3}";
    AIResponse treatRaw = new AIResponse(treatJson, "1.0", "gemini", "gemini-1.5-pro", 60, 100, "STOP");
    AgentResponse treatResp = new AgentResponse(treatRaw, "TreatmentAgent", AgentCapability.TREATMENT, Map.of());

    when(agentGateway.execute(any(AgentRequest.class)))
        .thenReturn(diagResp)
        .thenReturn(knowResp)
        .thenReturn(triageResp)
        .thenReturn(treatResp);

    ClinicalWorkflowResult result = workflowEngine.executeWorkflow(request);

    assertNotNull(result);
    assertEquals(WorkflowStatus.SUCCESS, result.status());
    assertNotNull(result.report());
    assertEquals("Bovine Foot and Mouth Disease", result.report().primaryDiagnosis());
    assertEquals(scanId, result.report().scanId());
    assertEquals(animalId, result.report().animalId());
    assertTrue(result.report().totalDurationMs() >= 0);

    // Verify event publications
    verify(eventPublisher).publishEvent(any(ClinicalWorkflowStartedEvent.class));
    verify(eventPublisher).publishEvent(any(ClinicalWorkflowCompletedEvent.class));

    // Verify metrics and span events
    verify(metricsCollector).recordClinicalWorkflow(any(), any(Long.class));
  }
}
