package app.vetra.ai.workflow.clinical.clinicalcase.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.ai.workflow.clinical.clinicalcase.response.TreatmentResponseStatus;
import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClinicalCaseStatusSummaryTest {

  @Test
  void testCanonicalConstructorDefaults() {
    UUID caseId = UUID.randomUUID();
    ClinicalCaseStatusSummary summary =
        new ClinicalCaseStatusSummary(
            caseId,
            ClinicalCaseStatus.UNDER_TREATMENT,
            TriageUrgency.URGENT,
            "Bovine Pneumonia",
            BigDecimal.valueOf(0.92),
            TreatmentResponseStatus.STABLE,
            true,
            2,
            Instant.now(),
            Instant.now().plusSeconds(86400));

    assertNotNull(summary);
    assertEquals(caseId, summary.caseId());
    assertEquals(ClinicalCaseStatus.UNDER_TREATMENT, summary.currentStatus());
    assertEquals(TriageUrgency.URGENT, summary.latestUrgency());
    assertEquals("Bovine Pneumonia", summary.latestDiagnosis());
    assertTrue(summary.veterinarianReviewRequired());
    assertEquals(2, summary.openFollowUps());
  }
}
