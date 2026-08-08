package app.vetra.ai.workflow.clinical.model.evidence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Immutable collection container carrying all normalized multi-modal clinical evidence items,
 * conflict warnings, data quality alerts, and synthesized summary representations.
 *
 * @param items list of normalized clinical evidence items
 * @param conflicts list of detected measurement conflicts
 * @param warnings list of data quality warnings
 * @param aggregatedAt aggregation timestamp
 */
public record UnifiedClinicalEvidence(
    List<ClinicalEvidence> items,
    List<String> conflicts,
    List<String> warnings,
    Instant aggregatedAt) {

  /** Canonical constructor with non-null defaults. */
  public UnifiedClinicalEvidence {
    items = items != null ? List.copyOf(items) : List.of();
    conflicts = conflicts != null ? List.copyOf(conflicts) : List.of();
    warnings = warnings != null ? List.copyOf(warnings) : List.of();
    aggregatedAt = aggregatedAt != null ? aggregatedAt : Instant.now();
  }

  /**
   * Filters evidence items by type.
   *
   * @param type evidence modality filter
   * @return list of matching {@link ClinicalEvidence} items
   */
  public List<ClinicalEvidence> findByType(EvidenceType type) {
    if (type == null) {
      return List.of();
    }
    return items.stream().filter(i -> i.type() == type).toList();
  }

  /**
   * Filters evidence items by source provenance.
   *
   * @param source evidence source filter
   * @return list of matching {@link ClinicalEvidence} items
   */
  public List<ClinicalEvidence> findBySource(EvidenceSource source) {
    if (source == null) {
      return List.of();
    }
    return items.stream().filter(i -> i.source() == source).toList();
  }

  /**
   * Checks whether any evidence item carries CRITICAL abnormality status.
   *
   * @return true if critical abnormalities exist
   */
  public boolean hasCriticalAbnormalities() {
    return items.stream().anyMatch(i -> i.status() == AbnormalityStatus.CRITICAL);
  }

  /**
   * Generates a controlled, concise clinical summary suitable for RAG queries and report assembly.
   *
   * @return formatted clinical summary string
   */
  public String toClinicalSummaryText() {
    List<String> lines = new ArrayList<>();

    List<ClinicalEvidence> symptoms = findByType(EvidenceType.SYMPTOM);
    if (!symptoms.isEmpty()) {
      lines.add("Symptoms: " + symptoms.get(0).summary());
    }

    List<ClinicalEvidence> vision = findByType(EvidenceType.IMAGE);
    if (!vision.isEmpty()) {
      lines.add("Visual Pathology: " + vision.get(0).summary());
    }

    List<ClinicalEvidence> labs = findByType(EvidenceType.LAB_RESULT);
    if (!labs.isEmpty()) {
      List<String> labSummaries = labs.stream().map(ClinicalEvidence::summary).toList();
      lines.add("Lab Results: " + String.join("; ", labSummaries));
    }

    List<ClinicalEvidence> vitals = findByType(EvidenceType.VITAL_SIGN);
    if (!vitals.isEmpty()) {
      List<String> vitalSummaries = vitals.stream().map(ClinicalEvidence::summary).toList();
      lines.add("Vital Signs: " + String.join("; ", vitalSummaries));
    }

    List<ClinicalEvidence> history = findByType(EvidenceType.CLINICAL_HISTORY);
    if (!history.isEmpty()) {
      List<String> histSummaries = history.stream().map(ClinicalEvidence::summary).toList();
      lines.add("Medical History: " + String.join("; ", histSummaries));
    }

    if (!conflicts.isEmpty()) {
      lines.add("Conflict Alerts: " + String.join("; ", conflicts));
    }

    return lines.isEmpty() ? "No multi-modal clinical evidence available." : String.join(" | ", lines);
  }
}
