package app.vetra.ai.workflow.clinical.model.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClinicalActionPlanTest {

  @Test
  void testCanonicalConstructorWithDefaults() {
    ClinicalActionPlan plan =
        new ClinicalActionPlan(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            TriageUrgency.EMERGENCY,
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            new FollowUpPlan("Check", "24 hours", List.of("Appetite"), List.of("Fever"), ActionActor.FARMER),
            true,
            "Immediate vet attention needed",
            null,
            null);

    assertNotNull(plan.planId());
    assertEquals(TriageUrgency.EMERGENCY, plan.urgency());
    assertTrue(plan.veterinarianReviewRequired());
    assertEquals("Immediate vet attention needed", plan.escalationSummary());
    assertNotNull(plan.generatedAt());
  }
}
