package app.vetra.ai.workflow.clinical.model.explainability;

import java.util.List;

/**
 * Deterministic decision criteria indicating whether professional veterinarian review is required.
 *
 * @param requiresReview true if any deterministic review rule triggered
 * @param reasons detailed textual explanations for review requirement
 * @param reasonCategories structured review trigger categories
 */
public record VeterinarianReviewFlag(
    boolean requiresReview,
    List<String> reasons,
    List<ReviewReasonCategory> reasonCategories) {

  /** Canonical constructor with non-null defaults. */
  public VeterinarianReviewFlag {
    reasons = reasons != null ? List.copyOf(reasons) : List.of();
    reasonCategories = reasonCategories != null ? List.copyOf(reasonCategories) : List.of();
  }
}
