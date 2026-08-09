package app.vetra.ai.workflow.clinical.model.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ClinicalActionTest {

  @Test
  void testCanonicalConstructorWithDefaults() {
    ClinicalAction action =
        new ClinicalAction(
            null,
            ActionType.MEDICATION,
            ActionPriority.HIGH,
            ActionActor.FARMER,
            " Administer Antibiotics ",
            " Administer penicillin as prescribed. ",
            List.of(),
            List.of("Check for allergies"),
            List.of(),
            List.of(),
            true,
            false,
            null,
            "TREATMENT_PLAN");

    assertNotNull(action.actionId());
    assertEquals(ActionType.MEDICATION, action.type());
    assertEquals(ActionPriority.HIGH, action.priority());
    assertEquals(ActionActor.FARMER, action.actor());
    assertEquals("Administer Antibiotics", action.title());
    assertEquals("Administer penicillin as prescribed.", action.description());
    assertTrue(action.mandatory());
    assertEquals("TREATMENT_PLAN", action.provenance());
  }
}
