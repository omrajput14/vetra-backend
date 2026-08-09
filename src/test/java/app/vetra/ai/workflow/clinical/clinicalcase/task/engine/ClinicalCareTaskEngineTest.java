package app.vetra.ai.workflow.clinical.clinicalcase.task.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.ai.workflow.clinical.clinicalcase.encounter.ClinicalEncounter;
import app.vetra.ai.workflow.clinical.clinicalcase.encounter.ClinicalEncounterType;
import app.vetra.ai.workflow.clinical.clinicalcase.followup.ClinicalFollowUp;
import app.vetra.ai.workflow.clinical.clinicalcase.followup.FollowUpStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.model.ClinicalCase;
import app.vetra.ai.workflow.clinical.clinicalcase.model.ClinicalCaseStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.response.TreatmentResponse;
import app.vetra.ai.workflow.clinical.clinicalcase.response.TreatmentResponseStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskActor;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskPriority;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskType;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.ClinicalCareTask;
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

class ClinicalCareTaskEngineTest {

  private ClinicalCareTaskEngine engine;

  @BeforeEach
  void setUp() {
    engine = new ClinicalCareTaskEngine();
  }

  @Test
  void testGenerateEmergencyTask_whenUrgencyIsEmergency() {
    UUID animalId = UUID.randomUUID();
    ClinicalCase c = new ClinicalCase(UUID.randomUUID(), animalId, "BOVINE", "Holstein", "Mastitis", Instant.now(), Instant.now(), null, ClinicalCaseStatus.OPEN);

    ClinicalEncounter enc = new ClinicalEncounter(
        UUID.randomUUID(), c.caseId(), UUID.randomUUID(), Instant.now(), ClinicalEncounterType.INITIAL_ASSESSMENT, TriageUrgency.EMERGENCY, "Acute Mastitis", BigDecimal.valueOf(0.95), List.of(), null, null, null, null);

    List<ClinicalCareTask> tasks = engine.generateCareTasks(c, enc, null, null, null, null);

    assertNotNull(tasks);
    assertFalse(tasks.isEmpty());
    ClinicalCareTask emergencyTask = tasks.get(0);
    assertEquals(CareTaskType.EMERGENCY_ESCALATION, emergencyTask.type());
    assertEquals(CareTaskPriority.EMERGENCY, emergencyTask.priority());
    assertEquals(CareTaskActor.VETERINARIAN, emergencyTask.actor());
    assertTrue(emergencyTask.mandatory());
    assertTrue(emergencyTask.veterinarianRequired());
    assertTrue(emergencyTask.escalationRequired());
  }

  @Test
  void testGenerateVetReviewTask_whenReviewFlagIsRequired() {
    UUID animalId = UUID.randomUUID();
    ClinicalCase c = new ClinicalCase(UUID.randomUUID(), animalId, "BOVINE", "Holstein", "Ketosis", Instant.now(), Instant.now(), null, ClinicalCaseStatus.OPEN);

    ClinicalEncounter enc = new ClinicalEncounter(
        UUID.randomUUID(), c.caseId(), UUID.randomUUID(), Instant.now(), ClinicalEncounterType.INITIAL_ASSESSMENT, TriageUrgency.URGENT, "Bovine Ketosis", BigDecimal.valueOf(0.70), List.of(), null, null, null, null);

    VeterinarianReviewFlag flag = new VeterinarianReviewFlag(true, List.of("High uncertainty in lab evidence"), List.of());
    ClinicalDecisionSupport cds = new ClinicalDecisionSupport(
        "Ketosis suspicious", List.of(), null, null, null, null, flag, Map.of(), Instant.now());

    List<ClinicalCareTask> tasks = engine.generateCareTasks(c, enc, null, cds, null, null);

    assertNotNull(tasks);
    assertTrue(tasks.stream().anyMatch(t -> t.type() == CareTaskType.VETERINARIAN_REVIEW && t.priority() == CareTaskPriority.HIGH));
  }

  @Test
  void testGenerateWorseningResponseTask_whenStatusIsWorsening() {
    UUID animalId = UUID.randomUUID();
    ClinicalCase c = new ClinicalCase(UUID.randomUUID(), animalId, "EQUINE", "Arabian", "Colic", Instant.now(), Instant.now(), null, ClinicalCaseStatus.UNDER_TREATMENT);

    ClinicalEncounter enc = new ClinicalEncounter(
        UUID.randomUUID(), c.caseId(), UUID.randomUUID(), Instant.now(), ClinicalEncounterType.FOLLOW_UP, TriageUrgency.URGENT, "Equine Colic", BigDecimal.valueOf(0.85), List.of(), null, null, null, null);

    TreatmentResponse response = new TreatmentResponse(
        UUID.randomUUID(), c.caseId(), UUID.randomUUID(), enc.encounterId(), TreatmentResponseStatus.WORSENING, List.of(), List.of("Triage urgency escalated"), List.of(), List.of(), Instant.now());

    List<ClinicalCareTask> tasks = engine.generateCareTasks(c, enc, null, null, null, response);

    assertNotNull(tasks);
    assertTrue(tasks.stream().anyMatch(t -> t.type() == CareTaskType.TREATMENT_REVIEW && t.priority() == CareTaskPriority.HIGH && t.escalationRequired()));
  }

  @Test
  void testGenerateFollowUpTask_fromExistingFollowUp() {
    UUID animalId = UUID.randomUUID();
    ClinicalCase c = new ClinicalCase(UUID.randomUUID(), animalId, "BOVINE", "Angus", "Pneumonia", Instant.now(), Instant.now(), null, ClinicalCaseStatus.UNDER_TREATMENT);

    ClinicalEncounter enc = new ClinicalEncounter(
        UUID.randomUUID(), c.caseId(), UUID.randomUUID(), Instant.now(), ClinicalEncounterType.INITIAL_ASSESSMENT, TriageUrgency.PRIORITY, "Bovine Pneumonia", BigDecimal.valueOf(0.88), List.of(), null, null, null, null);

    ClinicalFollowUp followUp = new ClinicalFollowUp(
        UUID.randomUUID(), c.caseId(), enc.encounterId(), Instant.now().plusSeconds(86400), null, FollowUpStatus.SCHEDULED, "Re-assess lung sounds", List.of("Lethargy"), List.of("Fever > 40C"));

    List<ClinicalCareTask> tasks = engine.generateCareTasks(c, enc, null, null, List.of(followUp), null);

    assertNotNull(tasks);
    assertTrue(tasks.stream().anyMatch(t -> t.type() == CareTaskType.FOLLOW_UP && t.title().contains("Follow-Up")));
  }

  @Test
  void testPrioritySortingPrecedence() {
    UUID animalId = UUID.randomUUID();
    ClinicalCase c = new ClinicalCase(UUID.randomUUID(), animalId, "BOVINE", "Holstein", "Pneumonia", Instant.now(), Instant.now(), null, ClinicalCaseStatus.OPEN);

    ClinicalEncounter enc = new ClinicalEncounter(
        UUID.randomUUID(), c.caseId(), UUID.randomUUID(), Instant.now(), ClinicalEncounterType.INITIAL_ASSESSMENT, TriageUrgency.EMERGENCY, "Severe Pneumonia", BigDecimal.valueOf(0.95), List.of(), null, null, null, null);

    VeterinarianReviewFlag flag = new VeterinarianReviewFlag(true, List.of("Emergency review required"), List.of());
    ClinicalDecisionSupport cds = new ClinicalDecisionSupport(
        "Emergency condition", List.of(), null, null, null, null, flag, Map.of(), Instant.now());

    List<ClinicalCareTask> tasks = engine.generateCareTasks(c, enc, null, cds, null, null);

    assertNotNull(tasks);
    assertEquals(CareTaskPriority.EMERGENCY, tasks.get(0).priority());
  }
}
