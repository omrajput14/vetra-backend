package app.vetra.ai.workflow.clinical.clinicalcase.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.vetra.ai.workflow.clinical.clinicalcase.model.ClinicalCaseStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskActor;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskPriority;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.CareTaskType;
import app.vetra.ai.workflow.clinical.clinicalcase.task.model.ClinicalCareTask;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FarmerOperationalCaseViewTest {

  @Test
  void testFarmerViewDataMinimization_excludesInternalAIandTraceabilityDetails() {
    UUID caseId = UUID.randomUUID();
    UUID animalId = UUID.randomUUID();
    Instant now = Instant.now();

    ClinicalCareTask careTask = new ClinicalCareTask(
        UUID.randomUUID(), caseId, UUID.randomUUID(), CareTaskType.MONITORING, CareTaskPriority.HIGH, CareTaskActor.CAREGIVER, CareTaskStatus.PENDING, "Check Temperature Daily", "Record morning temp", now, now.plusSeconds(86400), null, true, false, false, List.of(), List.of(), null, null, "ENGINE");

    FarmerOperationalCaseView farmerView = new FarmerOperationalCaseView(
        caseId,
        animalId,
        "BOVINE",
        "Holstein",
        "Mastitis",
        ClinicalCaseStatus.UNDER_TREATMENT,
        false,
        List.of(careTask),
        List.of(),
        "SCHEDULED",
        false,
        "Check Temperature Daily",
        now);

    assertEquals(caseId, farmerView.caseId());
    assertEquals(animalId, farmerView.animalId());
    assertEquals("BOVINE", farmerView.species());
    assertEquals("Holstein", farmerView.breed());
    assertEquals("Mastitis", farmerView.primaryCondition());
    assertEquals(ClinicalCaseStatus.UNDER_TREATMENT, farmerView.status());
    assertFalse(farmerView.veterinarianReviewRequired());
    assertEquals(1, farmerView.immediateCareTasks().size());
    assertEquals("Check Temperature Daily", farmerView.nextDueAction());
  }
}
