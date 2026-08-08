package app.vetra.ai.workflow.clinical.model.action;

import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Complete, auditable operational action plan synthesized deterministically from workflow context.
 *
 * @param planId unique plan identifier
 * @param scanId linked diagnostic scan identifier
 * @param animalId linked animal identifier
 * @param urgency triage urgency classification
 * @param immediateActions high-priority emergency or immediate care actions
 * @param prioritizedActions general prioritized care actions
 * @param monitoringActions ongoing monitoring actions
 * @param followUpActions scheduled follow-up actions
 * @param followUpPlan structured follow-up schedule summary
 * @param veterinarianReviewRequired true if veterinarian review/referral is required
 * @param escalationSummary concise summary of escalation guidance
 * @param generatedAt timestamp of plan generation
 * @param provenanceMetadata safe structural audit metadata
 */
public record ClinicalActionPlan(
    UUID planId,
    UUID scanId,
    UUID animalId,
    TriageUrgency urgency,
    List<ClinicalAction> immediateActions,
    List<ClinicalAction> prioritizedActions,
    List<ClinicalAction> monitoringActions,
    List<ClinicalAction> followUpActions,
    FollowUpPlan followUpPlan,
    boolean veterinarianReviewRequired,
    String escalationSummary,
    Instant generatedAt,
    Map<String, Object> provenanceMetadata) {

  /** Canonical constructor with non-null defaults. */
  public ClinicalActionPlan {
    planId = planId != null ? planId : UUID.randomUUID();
    urgency = urgency != null ? urgency : TriageUrgency.ROUTINE;
    immediateActions = immediateActions != null ? List.copyOf(immediateActions) : List.of();
    prioritizedActions = prioritizedActions != null ? List.copyOf(prioritizedActions) : List.of();
    monitoringActions = monitoringActions != null ? List.copyOf(monitoringActions) : List.of();
    followUpActions = followUpActions != null ? List.copyOf(followUpActions) : List.of();
    escalationSummary = escalationSummary != null ? escalationSummary.trim() : "";
    generatedAt = generatedAt != null ? generatedAt : Instant.now();
    provenanceMetadata = provenanceMetadata != null ? Map.copyOf(provenanceMetadata) : Map.of();
  }
}
