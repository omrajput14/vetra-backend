package app.vetra.ai.workflow.clinical.clinicalcase.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.ai.workflow.clinical.clinicalcase.encounter.ClinicalEncounter;
import app.vetra.ai.workflow.clinical.clinicalcase.encounter.ClinicalEncounterType;
import app.vetra.ai.workflow.clinical.clinicalcase.model.ClinicalCaseStatus;
import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import app.vetra.ai.workflow.clinical.model.explainability.ClinicalDecisionSupport;
import app.vetra.ai.workflow.clinical.model.explainability.VeterinarianReviewFlag;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VeterinarianOperationalCaseViewTest {

  @Test
  void testVeterinarianViewProjection_exposesCompleteTraceabilityState() {
    UUID caseId = UUID.randomUUID();
    UUID animalId = UUID.randomUUID();
    Instant now = Instant.now();

    ClinicalEncounter encounter = new ClinicalEncounter(
        UUID.randomUUID(), caseId, UUID.randomUUID(), now, ClinicalEncounterType.INITIAL_ASSESSMENT, TriageUrgency.URGENT, "Bovine Pneumonia", BigDecimal.valueOf(0.92), List.of(), null, null, null, null);

    VeterinarianReviewFlag flag = new VeterinarianReviewFlag(true, List.of("High uncertainty in lung auscultation"), List.of());
    ClinicalDecisionSupport cds = new ClinicalDecisionSupport("Pneumonia confirmed", List.of(), null, null, null, null, flag, Map.of(), now);

    VeterinarianOperationalCaseView vetView = new VeterinarianOperationalCaseView(
        caseId,
        animalId,
        "BOVINE",
        "Angus",
        "Pneumonia",
        ClinicalCaseStatus.OPEN,
        CaseOperationalStatus.VETERINARIAN_REVIEW_REQUIRED,
        encounter,
        "Bovine Pneumonia",
        BigDecimal.valueOf(0.92),
        TriageUrgency.URGENT,
        null,
        flag,
        false,
        "MODERATE",
        cds,
        List.of(),
        List.of(),
        null,
        "Veterinary Clinical Review Required",
        now);

    assertEquals(caseId, vetView.caseId());
    assertEquals(animalId, vetView.animalId());
    assertEquals("BOVINE", vetView.species());
    assertEquals("Angus", vetView.breed());
    assertEquals("Pneumonia", vetView.primaryCondition());
    assertEquals(CaseOperationalStatus.VETERINARIAN_REVIEW_REQUIRED, vetView.operationalStatus());
    assertEquals("Bovine Pneumonia", vetView.latestDiagnosis());
    assertEquals(BigDecimal.valueOf(0.92), vetView.diagnosticConfidence());
    assertEquals(TriageUrgency.URGENT, vetView.triageUrgency());
    assertNotNull(vetView.veterinarianReviewFlag());
    assertTrue(vetView.veterinarianReviewFlag().requiresReview());
    assertFalse(vetView.hasEvidenceConflicts());
    assertEquals("MODERATE", vetView.uncertaintyLevel());
    assertNotNull(vetView.decisionSupport());
    assertEquals("Veterinary Clinical Review Required", vetView.nextOperationalAction());
  }
}
