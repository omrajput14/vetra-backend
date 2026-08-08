package app.vetra.ai.workflow.clinical.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.ai.workflow.clinical.model.ClinicalWorkflowContext;
import app.vetra.ai.workflow.clinical.model.ClinicalWorkflowRequest;
import app.vetra.ai.workflow.clinical.model.TreatmentPlan;
import app.vetra.ai.workflow.clinical.model.TriageAssessment;
import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import app.vetra.ai.workflow.clinical.model.action.ActionActor;
import app.vetra.ai.workflow.clinical.model.action.ActionPriority;
import app.vetra.ai.workflow.clinical.model.action.ActionType;
import app.vetra.ai.workflow.clinical.model.action.ClinicalAction;
import app.vetra.ai.workflow.clinical.model.action.ClinicalActionPlan;
import app.vetra.ai.workflow.clinical.model.explainability.ClinicalDecisionSupport;
import app.vetra.ai.workflow.clinical.model.explainability.ClinicalUncertainty;
import app.vetra.ai.workflow.clinical.model.explainability.ContradictoryEvidenceSummary;
import app.vetra.ai.workflow.clinical.model.explainability.ReviewReasonCategory;
import app.vetra.ai.workflow.clinical.model.explainability.TriageExplanation;
import app.vetra.ai.workflow.clinical.model.explainability.TriageTriggerType;
import app.vetra.ai.workflow.clinical.model.explainability.TreatmentEvidence;
import app.vetra.ai.workflow.clinical.model.explainability.UncertaintyLevel;
import app.vetra.ai.workflow.clinical.model.explainability.VeterinarianReviewFlag;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClinicalActionPlanEngineTest {

  private ClinicalActionPlanEngine engine;

  @BeforeEach
  void setUp() {
    engine = new ClinicalActionPlanEngine();
  }

  @Test
  void testEmergencyTriage_producesMandatoryEmergencyVeterinaryReferral() {
    ClinicalWorkflowRequest request =
        new ClinicalWorkflowRequest(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "BOVINE",
            "Holstein",
            "https://cdn.vetra.app/img.jpg",
            List.of("Severe Respiratory Distress"),
            UUID.randomUUID(),
            null,
            null);

    ClinicalWorkflowContext context = new ClinicalWorkflowContext(request);

    TriageAssessment triage =
        new TriageAssessment(
            TriageUrgency.EMERGENCY,
            BigDecimal.valueOf(1.0),
            "Deterministic safety rule triggered emergency escalation",
            List.of("Severe Respiratory Distress"),
            List.of("Isolate animal immediately", "Administer oxygen"),
            true,
            Instant.now());
    context.setTriageAssessment(triage);

    ClinicalActionPlan plan = engine.synthesizePlan(context);

    assertNotNull(plan);
    assertEquals(TriageUrgency.EMERGENCY, plan.urgency());
    assertTrue(plan.veterinarianReviewRequired());
    assertFalse(plan.immediateActions().isEmpty());

    ClinicalAction referral = plan.immediateActions().get(0);
    assertEquals(ActionType.VETERINARY_REFERRAL, referral.type());
    assertEquals(ActionPriority.EMERGENCY, referral.priority());
    assertEquals(ActionActor.VETERINARIAN, referral.actor());
    assertTrue(referral.mandatory());
    assertTrue(referral.veterinarianRequired());
  }

  @Test
  void testVeterinarianReviewFlag_isPreservedAndCannotBeDowngraded() {
    ClinicalWorkflowRequest request =
        new ClinicalWorkflowRequest(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "BOVINE",
            "Angus",
            "",
            List.of("Mild Fever"),
            UUID.randomUUID(),
            null,
            null);

    ClinicalWorkflowContext context = new ClinicalWorkflowContext(request);

    TriageAssessment triage =
        new TriageAssessment(
            TriageUrgency.URGENT,
            BigDecimal.valueOf(0.85),
            "Urgent fever escalation",
            List.of("Fever > 40C"),
            List.of("Monitor temperature"),
            true,
            Instant.now());
    context.setTriageAssessment(triage);

    VeterinarianReviewFlag reviewFlag =
        new VeterinarianReviewFlag(
            true,
            List.of("Critical laboratory abnormality detected"),
            List.of(ReviewReasonCategory.CRITICAL_LAB_OR_VITAL));

    ClinicalDecisionSupport cds =
        new ClinicalDecisionSupport(
            "Primary Conclusion",
            List.of(),
            new TriageExplanation(TriageUrgency.URGENT, TriageTriggerType.AI_ASSESSMENT, List.of(), List.of(), "Urgent"),
            new TreatmentEvidence(List.of(), List.of(), List.of(), List.of()),
            new ClinicalUncertainty(UncertaintyLevel.MODERATE_CONFIDENCE, BigDecimal.valueOf(0.75), List.of(), List.of()),
            new ContradictoryEvidenceSummary(List.of(), List.of(), List.of()),
            reviewFlag,
            Map.of(),
            Instant.now());
    context.setDecisionSupport(cds);

    ClinicalActionPlan plan = engine.synthesizePlan(context);

    assertTrue(plan.veterinarianReviewRequired());
    boolean hasReferral = plan.immediateActions().stream().anyMatch(a -> a.type() == ActionType.VETERINARY_REFERRAL)
        || plan.prioritizedActions().stream().anyMatch(a -> a.type() == ActionType.VETERINARY_REFERRAL);
    assertTrue(hasReferral);
  }

  @Test
  void testMissingTreatmentPlan_representsLimitationWithoutFabricatingFacts() {
    ClinicalWorkflowRequest request =
        new ClinicalWorkflowRequest(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "EQUINE",
            "Arabian",
            "",
            List.of("Lethargy"),
            UUID.randomUUID(),
            null,
            null);

    ClinicalWorkflowContext context = new ClinicalWorkflowContext(request);

    ClinicalActionPlan plan = engine.synthesizePlan(context);

    assertNotNull(plan);
    boolean hasLimitationAction = plan.monitoringActions().stream()
        .anyMatch(a -> a.title().contains("Await Treatment Plan") || a.description().contains("unavailable"));
    assertTrue(hasLimitationAction);
  }

  @Test
  void testExactTreatmentProvenance_preservesMedicationNamesAndDetails() {
    ClinicalWorkflowRequest request =
        new ClinicalWorkflowRequest(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "BOVINE",
            "Holstein",
            "",
            List.of("Mastitis"),
            UUID.randomUUID(),
            null,
            null);

    ClinicalWorkflowContext context = new ClinicalWorkflowContext(request);

    TreatmentPlan treatmentPlan =
        new TreatmentPlan(
            "Intramammary Infusion Protocol",
            List.of("Cefapirin sodium 300mg"),
            List.of("Withhold milk for 96 hours"),
            List.of("Check somatic cell count daily"),
            3);
    context.setTreatmentPlan(treatmentPlan);

    ClinicalActionPlan plan = engine.synthesizePlan(context);

    assertNotNull(plan);
    boolean hasMedication = plan.prioritizedActions().stream()
        .anyMatch(a -> a.type() == ActionType.MEDICATION && a.title().contains("Cefapirin sodium 300mg"));
    assertTrue(hasMedication);
  }

  @Test
  void testIsolationAction_onlyGeneratedWhenSupportedByWorkflowState() {
    ClinicalWorkflowRequest request =
        new ClinicalWorkflowRequest(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "BOVINE",
            "Jersey",
            "",
            List.of("Fever"),
            UUID.randomUUID(),
            null,
            null);

    ClinicalWorkflowContext context = new ClinicalWorkflowContext(request);

    TriageAssessment triageWithIsolation =
        new TriageAssessment(
            TriageUrgency.URGENT,
            BigDecimal.valueOf(0.90),
            "FMD Risk",
            List.of("Isolate animal immediately from herd"),
            List.of("Quarantine pen setup"),
            true,
            Instant.now());
    context.setTriageAssessment(triageWithIsolation);

    ClinicalActionPlan plan = engine.synthesizePlan(context);

    boolean hasIsolation = plan.immediateActions().stream().anyMatch(a -> a.type() == ActionType.ISOLATION)
        || plan.prioritizedActions().stream().anyMatch(a -> a.type() == ActionType.ISOLATION);
    assertTrue(hasIsolation);
  }

  @Test
  void testDeduplication_removesDuplicateActionsWithIdenticalKey() {
    ClinicalWorkflowRequest request =
        new ClinicalWorkflowRequest(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "BOVINE",
            "Holstein",
            "",
            List.of("High fever"),
            UUID.randomUUID(),
            null,
            null);

    ClinicalWorkflowContext context = new ClinicalWorkflowContext(request);

    TreatmentPlan treatmentPlan =
        new TreatmentPlan(
            "Supportive Wash Protocol",
            List.of("Flunixin meglumine", "Flunixin meglumine"),
            List.of("Stay hydrated"),
            List.of("Feed intake"),
            2);
    context.setTreatmentPlan(treatmentPlan);

    ClinicalActionPlan plan = engine.synthesizePlan(context);

    long flunixinCount = plan.prioritizedActions().stream()
        .filter(a -> a.title().contains("Flunixin meglumine"))
        .count();
    assertEquals(1, flunixinCount);
  }
}
