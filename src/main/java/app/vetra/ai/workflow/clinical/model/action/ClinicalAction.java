package app.vetra.ai.workflow.clinical.model.action;

import app.vetra.ai.rag.model.Citation;
import app.vetra.ai.workflow.clinical.model.evidence.ClinicalEvidence;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Immutable operational action derived deterministically from workflow state.
 *
 * @param actionId unique action identifier
 * @param type functional action classification
 * @param priority action execution urgency tier
 * @param actor target responsible role
 * @param title concise actionable summary
 * @param description detailed instruction
 * @param prerequisites operational preconditions
 * @param warnings contraindications or safety precautions
 * @param supportingCitations RAG literature citations
 * @param supportingEvidence underlying clinical evidence items
 * @param mandatory true if action cannot be bypassed
 * @param veterinarianRequired true if veterinarian execution/review is required
 * @param dueAt target execution deadline (optional)
 * @param provenance source workflow component/field origin
 */
public record ClinicalAction(
    String actionId,
    ActionType type,
    ActionPriority priority,
    ActionActor actor,
    String title,
    String description,
    List<String> prerequisites,
    List<String> warnings,
    List<Citation> supportingCitations,
    List<ClinicalEvidence> supportingEvidence,
    boolean mandatory,
    boolean veterinarianRequired,
    Instant dueAt,
    String provenance) {

  /** Canonical constructor with non-null defaults. */
  public ClinicalAction {
    actionId = actionId != null ? actionId : UUID.randomUUID().toString();
    type = type != null ? type : ActionType.MONITORING;
    priority = priority != null ? priority : ActionPriority.MEDIUM;
    actor = actor != null ? actor : ActionActor.FARMER;
    title = title != null ? title.trim() : "Clinical Action";
    description = description != null ? description.trim() : "";
    prerequisites = prerequisites != null ? List.copyOf(prerequisites) : List.of();
    warnings = warnings != null ? List.copyOf(warnings) : List.of();
    supportingCitations = supportingCitations != null ? List.copyOf(supportingCitations) : List.of();
    supportingEvidence = supportingEvidence != null ? List.copyOf(supportingEvidence) : List.of();
    provenance = provenance != null ? provenance.trim() : "WORKFLOW_STATE";
  }
}
