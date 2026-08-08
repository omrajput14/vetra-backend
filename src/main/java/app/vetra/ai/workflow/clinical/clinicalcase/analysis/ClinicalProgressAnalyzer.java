package app.vetra.ai.workflow.clinical.clinicalcase.analysis;

import app.vetra.ai.workflow.clinical.clinicalcase.encounter.ClinicalEncounter;
import app.vetra.ai.workflow.clinical.clinicalcase.response.TreatmentResponse;
import app.vetra.ai.workflow.clinical.clinicalcase.response.TreatmentResponseStatus;
import app.vetra.ai.workflow.clinical.model.evidence.AbnormalityStatus;
import app.vetra.ai.workflow.clinical.model.evidence.ClinicalEvidence;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Pure deterministic non-AI longitudinal progress analysis component comparing like-for-like
 * evidence across encounters without inventing clinical interpretations or assuming recovery.
 */
@Component
public class ClinicalProgressAnalyzer {

  private static final Logger log = LoggerFactory.getLogger(ClinicalProgressAnalyzer.class);

  /**
   * Analyzes treatment response between a previous encounter and current encounter.
   *
   * @param previousEncounter previous historical encounter (optional)
   * @param currentEncounter current encounter
   * @return deterministic {@link TreatmentResponse}
   */
  public TreatmentResponse analyzeProgress(ClinicalEncounter previousEncounter, ClinicalEncounter currentEncounter) {
    if (currentEncounter == null) {
      throw new IllegalArgumentException("Current ClinicalEncounter cannot be null");
    }

    if (previousEncounter == null) {
      log.debug("No previous encounter available for caseId={}. Returning INSUFFICIENT_DATA.", currentEncounter.caseId());
      return new TreatmentResponse(
          UUID.randomUUID(),
          currentEncounter.caseId(),
          null,
          currentEncounter.encounterId(),
          TreatmentResponseStatus.INSUFFICIENT_DATA,
          List.of(),
          List.of(),
          List.of("Initial baseline encounter recorded"),
          List.of(),
          Instant.now());
    }

    List<String> supporting = new ArrayList<>();
    List<String> worsening = new ArrayList<>();
    List<String> unchanged = new ArrayList<>();
    List<ClinicalEvidence> supportingEvidence = new ArrayList<>();

    compareTriageAndConfidence(previousEncounter, currentEncounter, supporting, worsening, unchanged);
    compareEvidenceItems(previousEncounter, currentEncounter, supporting, worsening, unchanged, supportingEvidence);

    TreatmentResponseStatus status = determineResponseStatus(supporting, worsening, unchanged, supportingEvidence);

    log.info(
        "ClinicalProgressAnalyzer completed for caseId={}: status={}, supporting={}, worsening={}",
        currentEncounter.caseId(),
        status,
        supporting.size(),
        worsening.size());

    return new TreatmentResponse(
        UUID.randomUUID(),
        currentEncounter.caseId(),
        previousEncounter.encounterId(),
        currentEncounter.encounterId(),
        status,
        supporting,
        worsening,
        unchanged,
        supportingEvidence,
        Instant.now());
  }

  private void compareTriageAndConfidence(
      ClinicalEncounter prev,
      ClinicalEncounter curr,
      List<String> supporting,
      List<String> worsening,
      List<String> unchanged) {

    int prevRank = getUrgencyRank(prev.urgency());
    int currRank = getUrgencyRank(curr.urgency());

    if (currRank > prevRank) {
      worsening.add(String.format("Triage urgency escalated from %s to %s", prev.urgency(), curr.urgency()));
    } else if (currRank < prevRank) {
      supporting.add(String.format("Triage urgency de-escalated from %s to %s", prev.urgency(), curr.urgency()));
    } else {
      unchanged.add(String.format("Triage urgency stable at %s", curr.urgency()));
    }

    double diff = curr.diagnosticConfidence().doubleValue() - prev.diagnosticConfidence().doubleValue();
    if (Math.abs(diff) >= 0.10) {
      if (diff > 0) {
        supporting.add(String.format("Diagnostic confidence score increased from %.2f to %.2f", prev.diagnosticConfidence().doubleValue(), curr.diagnosticConfidence().doubleValue()));
      } else {
        worsening.add(String.format("Diagnostic confidence score decreased from %.2f to %.2f", prev.diagnosticConfidence().doubleValue(), curr.diagnosticConfidence().doubleValue()));
      }
    }
  }

  private void compareEvidenceItems(
      ClinicalEncounter prev,
      ClinicalEncounter curr,
      List<String> supporting,
      List<String> worsening,
      List<String> unchanged,
      List<ClinicalEvidence> supportingEvidence) {

    if (prev.evidenceSummary() == null || curr.evidenceSummary() == null) {
      return;
    }

    List<ClinicalEvidence> prevItems = getEvidenceItemsFromEncounter(prev);
    List<ClinicalEvidence> currItems = getEvidenceItemsFromEncounter(curr);

    for (ClinicalEvidence currItem : currItems) {
      for (ClinicalEvidence prevItem : prevItems) {
        if (currItem.type() == prevItem.type() && isMatchingEvidence(currItem, prevItem)) {
          evaluateLikeForLikeEvidence(prevItem, currItem, supporting, worsening, unchanged, supportingEvidence);
        }
      }
    }
  }

  private List<ClinicalEvidence> getEvidenceItemsFromEncounter(ClinicalEncounter encounter) {
    if (encounter.decisionSupport() != null && encounter.decisionSupport().treatmentEvidence() != null) {
      return encounter.decisionSupport().treatmentEvidence().supportingEvidence();
    }
    return List.of();
  }

  private boolean isMatchingEvidence(ClinicalEvidence a, ClinicalEvidence b) {
    if (a.summary() != null && b.summary() != null && !a.summary().isBlank() && !b.summary().isBlank()) {
      return a.summary().equalsIgnoreCase(b.summary());
    }
    return false;
  }

  private void evaluateLikeForLikeEvidence(
      ClinicalEvidence prev,
      ClinicalEvidence curr,
      List<String> supporting,
      List<String> worsening,
      List<String> unchanged,
      List<ClinicalEvidence> supportingEvidence) {

    if (curr.status() == AbnormalityStatus.CRITICAL && prev.status() != AbnormalityStatus.CRITICAL) {
      worsening.add(String.format("New critical abnormality detected in %s: %s", curr.type(), curr.summary()));
      supportingEvidence.add(curr);
    } else if (curr.status() == AbnormalityStatus.NORMAL && prev.status() != AbnormalityStatus.NORMAL) {
      supporting.add(String.format("Abnormality normalized in %s: %s", curr.type(), curr.summary()));
      supportingEvidence.add(curr);
    } else if (curr.status() == prev.status()) {
      unchanged.add(String.format("Evidence status stable for %s: %s (%s)", curr.type(), curr.summary(), curr.status()));
    }
  }

  private TreatmentResponseStatus determineResponseStatus(
      List<String> supporting,
      List<String> worsening,
      List<String> unchanged,
      List<ClinicalEvidence> supportingEvidence) {

    if (supporting.isEmpty() && worsening.isEmpty() && unchanged.isEmpty()) {
      return TreatmentResponseStatus.INSUFFICIENT_DATA;
    }

    if (!worsening.isEmpty()) {
      return TreatmentResponseStatus.WORSENING;
    }

    // Per Constraint 3: Reduced urgency or increased confidence alone MUST NOT mean IMPROVING.
    // IMPROVING requires concrete clinical evidence normalization in supportingEvidence.
    if (!supporting.isEmpty() && !supportingEvidence.isEmpty()) {
      return TreatmentResponseStatus.IMPROVING;
    }

    return TreatmentResponseStatus.STABLE;
  }

  private int getUrgencyRank(app.vetra.ai.workflow.clinical.model.TriageUrgency urgency) {
    return switch (urgency) {
      case EMERGENCY -> 4;
      case URGENT -> 3;
      case PRIORITY -> 2;
      case ROUTINE -> 1;
    };
  }
}
