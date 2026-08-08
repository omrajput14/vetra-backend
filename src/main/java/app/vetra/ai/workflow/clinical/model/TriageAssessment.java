package app.vetra.ai.workflow.clinical.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Immutable assessment record representing the clinical urgency classification, warning signs,
 * escalation advice, and confidence level.
 *
 * @param urgency classified urgency level (EMERGENCY, URGENT, PRIORITY, ROUTINE)
 * @param confidence calibrated assessment confidence normalized to [0.00, 1.00]
 * @param rationale clinical reasoning justifying the urgency classification
 * @param warningSigns identified red-flag clinical indicators or warning signs
 * @param recommendedActions actionable escalation guidance for farmer and veterinarian
 * @param requiresImmediateVeterinaryReview whether immediate vet contact is mandatory
 * @param assessedAt assessment timestamp
 */
public record TriageAssessment(
    TriageUrgency urgency,
    BigDecimal confidence,
    String rationale,
    List<String> warningSigns,
    List<String> recommendedActions,
    boolean requiresImmediateVeterinaryReview,
    Instant assessedAt) {

  /** Canonical constructor with non-null defaults. */
  public TriageAssessment {
    urgency = urgency != null ? urgency : TriageUrgency.ROUTINE;
    confidence = confidence != null ? confidence : BigDecimal.valueOf(0.50);
    rationale = rationale != null ? rationale.trim() : "Standard clinical evaluation.";
    warningSigns = warningSigns != null ? List.copyOf(warningSigns) : List.of();
    recommendedActions = recommendedActions != null ? List.copyOf(recommendedActions) : List.of();
    assessedAt = assessedAt != null ? assessedAt : Instant.now();
  }

  /**
   * Creates a conservative fallback assessment when AI classification is unavailable.
   *
   * @param reason failure description or rationale
   * @return conservative fallback assessment
   */
  public static TriageAssessment conservativeFallback(String reason) {
    return new TriageAssessment(
        TriageUrgency.URGENT,
        BigDecimal.valueOf(0.50),
        "Automated AI triage assessment unavailable (" + (reason != null ? reason : "unknown issue")
            + "). Assigned conservative URGENT classification pending veterinarian review.",
        List.of("Automated triage assessment unverified"),
        List.of("Contact a licensed veterinarian as soon as possible for professional evaluation"),
        true,
        Instant.now());
  }
}
