package app.vetra.ai.workflow.clinical.model.evidence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Normalized immutable clinical evidence item encapsulating findings across modalities (Image, Symptoms, Labs, Vitals, Sensors, History, RAG).
 *
 * @param id unique evidence identifier
 * @param type evidence modality classification
 * @param source evidence origin/provenance
 * @param summary concise textual summary of findings
 * @param observations granular structured observations
 * @param confidence confidence level if applicable (0.00 to 1.00)
 * @param status clinical abnormality status
 * @param timestamp observation timestamp
 * @param metadata non-sensitive metadata key-value pairs
 */
public record ClinicalEvidence(
    String id,
    EvidenceType type,
    EvidenceSource source,
    String summary,
    List<String> observations,
    BigDecimal confidence,
    AbnormalityStatus status,
    Instant timestamp,
    Map<String, String> metadata) {

  /** Canonical constructor with non-null defaults. */
  public ClinicalEvidence {
    id = id != null ? id : UUID.randomUUID().toString();
    type = type != null ? type : EvidenceType.SYMPTOM;
    source = source != null ? source : EvidenceSource.VET_OBSERVATION;
    summary = summary != null ? summary.trim() : "";
    observations = observations != null ? List.copyOf(observations) : List.of();
    confidence = confidence != null ? confidence : BigDecimal.valueOf(1.00);
    status = status != null ? status : AbnormalityStatus.NORMAL;
    timestamp = timestamp != null ? timestamp : Instant.now();
    metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
  }
}
