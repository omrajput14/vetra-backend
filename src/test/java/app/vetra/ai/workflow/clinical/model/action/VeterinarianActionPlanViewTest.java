package app.vetra.ai.workflow.clinical.model.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VeterinarianActionPlanViewTest {

  @Test
  void testFromActionPlan_projectsVeterinarianView() {
    ClinicalAction action =
        new ClinicalAction(
            "act-1",
            ActionType.VETERINARY_REFERRAL,
            ActionPriority.EMERGENCY,
            ActionActor.VETERINARIAN,
            "Emergency Visit",
            "Urgent attendance required",
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            true,
            true,
            null,
            "TEST");

    ClinicalActionPlan plan =
        new ClinicalActionPlan(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            TriageUrgency.EMERGENCY,
            List.of(action),
            List.of(),
            List.of(),
            List.of(),
            null,
            true,
            "Emergency",
            null,
            null);

    VeterinarianActionPlanView view = VeterinarianActionPlanView.fromActionPlan(plan);

    assertNotNull(view);
    assertTrue(view.veterinarianReviewRequired());
    assertEquals(1, view.totalActionCount());
    assertEquals(1, view.allActions().size());
  }
}
