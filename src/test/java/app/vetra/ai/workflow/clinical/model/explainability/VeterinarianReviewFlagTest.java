package app.vetra.ai.workflow.clinical.model.explainability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class VeterinarianReviewFlagTest {

  @Test
  void testReviewRequiredPrecedence() {
    VeterinarianReviewFlag flag =
        new VeterinarianReviewFlag(
            true,
            List.of("Emergency triage classification", "Critical vital sign detected"),
            List.of(ReviewReasonCategory.EMERGENCY_TRIAGE, ReviewReasonCategory.CRITICAL_LAB_OR_VITAL));

    assertTrue(flag.requiresReview());
    assertEquals(2, flag.reasonCategories().size());
    assertTrue(flag.reasonCategories().contains(ReviewReasonCategory.EMERGENCY_TRIAGE));
  }
}
