package app.vetra.ai.workflow.clinical.clinicalcase.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.ai.workflow.clinical.clinicalcase.model.ClinicalCaseStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.response.TreatmentResponseStatus;
import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClinicalCaseOperationalViewTest {

  @Test
  void testOperationalViewProjection_andNonFabricatedDefaults() {
    UUID caseId = UUID.randomUUID();
    UUID animalId = UUID.randomUUID();

    ClinicalCaseOperationalView view = new ClinicalCaseOperationalView(
        caseId,
        animalId,
        "BOVINE",
        "Holstein",
        "Bovine Mastitis",
        ClinicalCaseStatus.OPEN,
        CaseOperationalStatus.EMERGENCY,
        UUID.randomUUID(),
        Instant.now(),
        TriageUrgency.EMERGENCY,
        "Acute Mastitis",
        BigDecimal.valueOf(0.95),
        TreatmentResponseStatus.WORSENING,
        true,
        true,
        3,
        1,
        1,
        2,
        1,
        Instant.now().plusSeconds(3600),
        "Immediate Emergency Isolation",
        Instant.now());

    assertEquals(caseId, view.caseId());
    assertEquals(animalId, view.animalId());
    assertEquals("BOVINE", view.species());
    assertEquals("Holstein", view.breed());
    assertEquals("Bovine Mastitis", view.primaryCondition());
    assertEquals(ClinicalCaseStatus.OPEN, view.currentCaseStatus());
    assertEquals(CaseOperationalStatus.EMERGENCY, view.operationalStatus());
    assertEquals(TriageUrgency.EMERGENCY, view.latestUrgency());
    assertEquals("Acute Mastitis", view.latestDiagnosis());
    assertEquals(BigDecimal.valueOf(0.95), view.latestDiagnosticConfidence());
    assertEquals(TreatmentResponseStatus.WORSENING, view.treatmentResponseStatus());
    assertTrue(view.veterinarianReviewRequired());
    assertTrue(view.emergency());
    assertEquals(3, view.openTaskCount());
    assertEquals(1, view.overdueTaskCount());
    assertEquals(1, view.emergencyTaskCount());
    assertEquals(2, view.pendingFollowUpCount());
    assertEquals(1, view.overdueFollowUpCount());
    assertNotNull(view.nextDueAt());
    assertEquals("Immediate Emergency Isolation", view.nextOperationalAction());
  }

  @Test
  void testNullHandling_keepsDefaultsWithoutFabricatingFacts() {
    ClinicalCaseOperationalView view = new ClinicalCaseOperationalView(
        null, null, null, null, null, null, null, null, null, null, null, null, null, false, false, 0, 0, 0, 0, 0, null, null, null);

    assertNotNull(view.caseId());
    assertNotNull(view.animalId());
    assertEquals("UNKNOWN", view.species());
    assertEquals("UNKNOWN", view.breed());
    assertEquals("UNKNOWN", view.primaryCondition());
    assertEquals(ClinicalCaseStatus.OPEN, view.currentCaseStatus());
    assertEquals(CaseOperationalStatus.STABLE, view.operationalStatus());
    assertEquals(TriageUrgency.ROUTINE, view.latestUrgency());
    assertEquals("UNKNOWN", view.latestDiagnosis());
    assertEquals(BigDecimal.ZERO, view.latestDiagnosticConfidence());
    assertEquals(TreatmentResponseStatus.INSUFFICIENT_DATA, view.treatmentResponseStatus());
    assertFalse(view.veterinarianReviewRequired());
    assertFalse(view.emergency());
    assertEquals("", view.nextOperationalAction());
  }
}
