package app.vetra.ai.workflow.clinical.model.explainability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import java.util.List;
import org.junit.jupiter.api.Test;

class TriageExplanationTest {

  @Test
  void testDeterministicSafetyRuleTrigger() {
    TriageExplanation explanation =
        new TriageExplanation(
            TriageUrgency.EMERGENCY,
            TriageTriggerType.DETERMINISTIC_SAFETY_RULE,
            List.of("Severe Respiratory Distress"),
            List.of("Dyspnea", "Cyanosis"),
            "Deterministic safety rule triggered emergency escalation");

    assertEquals(TriageUrgency.EMERGENCY, explanation.assignedUrgency());
    assertEquals(TriageTriggerType.DETERMINISTIC_SAFETY_RULE, explanation.triggerType());
    assertEquals(1, explanation.triggeredRules().size());
  }
}
