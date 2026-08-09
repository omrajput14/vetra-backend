package app.vetra.ai.workflow.clinical.model.explainability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClinicalUncertaintyTest {

  @Test
  void testMissingModalitiesQuantification() {
    ClinicalUncertainty uncertainty =
        new ClinicalUncertainty(
            UncertaintyLevel.INSUFFICIENT_EVIDENCE,
            BigDecimal.valueOf(0.30),
            List.of("Missing laboratory and vital sign data"),
            List.of("LAB_RESULT", "VITAL_SIGN"));

    assertEquals(UncertaintyLevel.INSUFFICIENT_EVIDENCE, uncertainty.overallLevel());
    assertEquals(2, uncertainty.missingModalities().size());
    assertTrue(uncertainty.missingModalities().contains("LAB_RESULT"));
  }
}
