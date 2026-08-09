package app.vetra.ai.workflow.clinical.explainability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.ai.workflow.clinical.model.ClinicalWorkflowContext;
import app.vetra.ai.workflow.clinical.model.ClinicalWorkflowRequest;
import app.vetra.ai.workflow.clinical.model.DiseaseCandidate;
import app.vetra.ai.workflow.clinical.model.TriageAssessment;
import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import app.vetra.ai.workflow.clinical.model.evidence.AbnormalityStatus;
import app.vetra.ai.workflow.clinical.model.evidence.ClinicalEvidence;
import app.vetra.ai.workflow.clinical.model.evidence.EvidenceSource;
import app.vetra.ai.workflow.clinical.model.evidence.EvidenceType;
import app.vetra.ai.workflow.clinical.model.evidence.UnifiedClinicalEvidence;
import app.vetra.ai.workflow.clinical.model.explainability.ClinicalDecisionSupport;
import app.vetra.ai.workflow.clinical.model.explainability.ReviewReasonCategory;
import app.vetra.ai.workflow.clinical.model.explainability.TriageTriggerType;
import app.vetra.ai.workflow.clinical.model.explainability.UncertaintyLevel;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClinicalDecisionSupportEngineTest {

  private ClinicalDecisionSupportEngine engine;

  @BeforeEach
  void setUp() {
    engine = new ClinicalDecisionSupportEngine();
  }

  @Test
  void testEvaluate_withEmergencyTriageAndCriticalLabs() {
    ClinicalWorkflowRequest request =
        new ClinicalWorkflowRequest(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "BOVINE",
            "Holstein",
            "https://cdn.vetra.app/img.jpg",
            List.of("Severe Dyspnea"),
            UUID.randomUUID(),
            null,
            null);

    ClinicalWorkflowContext context = new ClinicalWorkflowContext(request);

    DiseaseCandidate candidate =
        new DiseaseCandidate("Bovine Respiratory Disease", BigDecimal.valueOf(0.92), "Severe lung lesions", List.of(), false);
    context.setRankedDiseases(List.of(candidate));

    ClinicalEvidence criticalLab =
        new ClinicalEvidence(
            "lab-1",
            EvidenceType.LAB_RESULT,
            EvidenceSource.LABORATORY,
            "WBC: 28k cells/uL (Status: CRITICAL)",
            List.of("WBC 28k"),
            BigDecimal.valueOf(1.0),
            AbnormalityStatus.CRITICAL,
            Instant.now(),
            Map.of());

    UnifiedClinicalEvidence unified =
        new UnifiedClinicalEvidence(List.of(criticalLab), List.of("Concurrent Temp Conflict"), List.of(), Instant.now());
    context.setUnifiedEvidence(unified);

    TriageAssessment triage =
        new TriageAssessment(
            TriageUrgency.EMERGENCY,
            BigDecimal.valueOf(1.0),
            "Deterministic emergency rule triggered: Severe Respiratory Distress",
            List.of("Severe Respiratory Distress"),
            List.of("Immediate Oxygen Therapy"),
            true,
            Instant.now());
    context.setTriageAssessment(triage);

    ClinicalDecisionSupport cds = engine.evaluate(context);

    assertNotNull(cds);
    assertEquals(TriageUrgency.EMERGENCY, cds.triageExplanation().assignedUrgency());
    assertEquals(TriageTriggerType.DETERMINISTIC_SAFETY_RULE, cds.triageExplanation().triggerType());
    assertTrue(cds.veterinarianReviewFlag().requiresReview());
    assertTrue(cds.veterinarianReviewFlag().reasonCategories().contains(ReviewReasonCategory.EMERGENCY_TRIAGE));
    assertTrue(cds.veterinarianReviewFlag().reasonCategories().contains(ReviewReasonCategory.CRITICAL_LAB_OR_VITAL));
    assertTrue(cds.veterinarianReviewFlag().reasonCategories().contains(ReviewReasonCategory.EVIDENCE_CONFLICT));
  }

  @Test
  void testEvaluate_missingEvidence_quantifiesUncertainty() {
    ClinicalWorkflowRequest request =
        new ClinicalWorkflowRequest(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "BOVINE",
            "Angus",
            "",
            List.of("Mild Cough"),
            UUID.randomUUID(),
            null,
            null);

    ClinicalWorkflowContext context = new ClinicalWorkflowContext(request);

    DiseaseCandidate lowConfCandidate =
        new DiseaseCandidate("Mild Bronchitis", BigDecimal.valueOf(0.35), "Vague symptoms", List.of(), false);
    context.setRankedDiseases(List.of(lowConfCandidate));

    ClinicalDecisionSupport cds = engine.evaluate(context);

    assertNotNull(cds);
    assertEquals(UncertaintyLevel.INSUFFICIENT_EVIDENCE, cds.uncertaintyAssessment().overallLevel());
    assertTrue(cds.uncertaintyAssessment().missingModalities().contains("LAB_RESULT"));
    assertTrue(cds.uncertaintyAssessment().missingModalities().contains("VITAL_SIGN"));
    assertTrue(cds.veterinarianReviewFlag().requiresReview());
    assertTrue(cds.veterinarianReviewFlag().reasonCategories().contains(ReviewReasonCategory.LOW_DIAGNOSTIC_CONFIDENCE));
  }
}
