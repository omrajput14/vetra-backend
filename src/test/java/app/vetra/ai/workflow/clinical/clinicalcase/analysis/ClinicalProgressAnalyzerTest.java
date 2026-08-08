package app.vetra.ai.workflow.clinical.clinicalcase.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.vetra.ai.workflow.clinical.clinicalcase.encounter.ClinicalEncounter;
import app.vetra.ai.workflow.clinical.clinicalcase.encounter.ClinicalEncounterType;
import app.vetra.ai.workflow.clinical.clinicalcase.response.TreatmentResponse;
import app.vetra.ai.workflow.clinical.clinicalcase.response.TreatmentResponseStatus;
import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClinicalProgressAnalyzerTest {

  private ClinicalProgressAnalyzer analyzer;

  @BeforeEach
  void setUp() {
    analyzer = new ClinicalProgressAnalyzer();
  }

  @Test
  void testNoPreviousEncounter_returnsInsufficientData() {
    ClinicalEncounter current =
        new ClinicalEncounter(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.now(),
            ClinicalEncounterType.INITIAL_ASSESSMENT,
            TriageUrgency.URGENT,
            "Mastitis",
            BigDecimal.valueOf(0.85),
            List.of(),
            null,
            null,
            null,
            null);

    TreatmentResponse response = analyzer.analyzeProgress(null, current);

    assertNotNull(response);
    assertEquals(TreatmentResponseStatus.INSUFFICIENT_DATA, response.status());
  }

  @Test
  void testReducedUrgencyAlone_doesNotAutomaticallyMeanImproving() {
    UUID caseId = UUID.randomUUID();
    ClinicalEncounter prev =
        new ClinicalEncounter(
            UUID.randomUUID(),
            caseId,
            UUID.randomUUID(),
            Instant.now().minusSeconds(86400),
            ClinicalEncounterType.INITIAL_ASSESSMENT,
            TriageUrgency.EMERGENCY,
            "Foot and Mouth",
            BigDecimal.valueOf(0.70),
            List.of(),
            null,
            null,
            null,
            null);

    ClinicalEncounter curr =
        new ClinicalEncounter(
            UUID.randomUUID(),
            caseId,
            UUID.randomUUID(),
            Instant.now(),
            ClinicalEncounterType.FOLLOW_UP,
            TriageUrgency.URGENT,
            "Foot and Mouth",
            BigDecimal.valueOf(0.70),
            List.of(),
            null,
            null,
            null,
            null);

    TreatmentResponse response = analyzer.analyzeProgress(prev, curr);

    assertNotNull(response);
    assertEquals(TreatmentResponseStatus.STABLE, response.status());
  }

  @Test
  void testIncreasedDiagnosticConfidenceAlone_doesNotAutomaticallyMeanImproving() {
    UUID caseId = UUID.randomUUID();
    ClinicalEncounter prev =
        new ClinicalEncounter(
            UUID.randomUUID(),
            caseId,
            UUID.randomUUID(),
            Instant.now().minusSeconds(86400),
            ClinicalEncounterType.INITIAL_ASSESSMENT,
            TriageUrgency.URGENT,
            "Pneumonia",
            BigDecimal.valueOf(0.60),
            List.of(),
            null,
            null,
            null,
            null);

    ClinicalEncounter curr =
        new ClinicalEncounter(
            UUID.randomUUID(),
            caseId,
            UUID.randomUUID(),
            Instant.now(),
            ClinicalEncounterType.FOLLOW_UP,
            TriageUrgency.URGENT,
            "Pneumonia",
            BigDecimal.valueOf(0.95),
            List.of(),
            null,
            null,
            null,
            null);

    TreatmentResponse response = analyzer.analyzeProgress(prev, curr);

    assertNotNull(response);
    assertEquals(TreatmentResponseStatus.STABLE, response.status());
  }
}
