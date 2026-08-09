package app.vetra.ai.workflow.clinical.model.explainability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DiagnosticExplanationTest {

  @Test
  void testCanonicalConstructorWithDefaults() {
    DiagnosticExplanation explanation =
        new DiagnosticExplanation(
            "Foot and Mouth",
            List.of(),
            List.of(),
            List.of(),
            Map.of("IMAGE", 0.35),
            BigDecimal.valueOf(0.85),
            UncertaintyLevel.HIGH_CONFIDENCE,
            List.of("Clear visual indicators"));

    assertEquals("Foot and Mouth", explanation.diseaseName());
    assertEquals(BigDecimal.valueOf(0.85), explanation.confidence());
    assertEquals(UncertaintyLevel.HIGH_CONFIDENCE, explanation.uncertaintyLevel());
    assertNotNull(explanation.supportingEvidence());
    assertNotNull(explanation.contradictoryEvidence());
    assertNotNull(explanation.citations());
  }
}
