package app.vetra.ai.workflow.clinical.model.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FarmerActionPlanViewTest {

  @Test
  void testFromActionPlan_projectsFarmerView() {
    ClinicalAction action =
        new ClinicalAction(
            "act-1",
            ActionType.IMMEDIATE_CARE,
            ActionPriority.HIGH,
            ActionActor.FARMER,
            "Isolate Cow",
            "Move to clean pen",
            List.of(),
            List.of("Biosecurity risk"),
            List.of(),
            List.of(),
            true,
            false,
            null,
            "TEST");

    ClinicalActionPlan plan =
        new ClinicalActionPlan(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            TriageUrgency.URGENT,
            List.of(action),
            List.of(),
            List.of(),
            List.of(),
            new FollowUpPlan("Check", "24 hours", List.of("Temp"), List.of("Cough"), ActionActor.FARMER),
            true,
            "Contact vet",
            null,
            null);

    FarmerActionPlanView view = FarmerActionPlanView.fromActionPlan(plan);

    assertNotNull(view);
    assertEquals(TriageUrgency.URGENT, view.urgency());
    assertTrue(view.veterinarianReferralRequired());
    assertEquals(1, view.immediateSteps().size());
    assertTrue(view.immediateSteps().get(0).contains("Isolate Cow"));
  }
}
