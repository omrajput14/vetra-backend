package app.vetra.ai.workflow.clinical.triage;

import app.vetra.ai.workflow.clinical.model.TriageAssessment;
import app.vetra.ai.workflow.clinical.model.TriageRequest;
import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import app.vetra.ai.workflow.clinical.model.evidence.AbnormalityStatus;
import app.vetra.ai.workflow.clinical.model.evidence.ClinicalEvidence;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Layer 1 Deterministic Triage Safety Rules.
 *
 * <p>Evaluates observable clinical symptoms, diagnostic findings, laboratory results, and vital signs
 * against critical emergency indicators with strict negation handling (e.g. "no respiratory distress" does not trigger an emergency).
 *
 * <p>Deterministic emergency rules take absolute precedence over AI output. If a critical indicator
 * is detected, this component returns an EMERGENCY assessment immediately without invoking AI agents.
 */
@Component
public class ClinicalTriageRules {

  private static final Logger log = LoggerFactory.getLogger(ClinicalTriageRules.class);

  private static final Set<String> CRITICAL_RESPIRATORY_KEYWORDS =
      Set.of("respiratory distress", "dyspnea", "gasping for air", "asphyxia", "cyanosis");

  private static final Set<String> CRITICAL_HEMORRHAGE_KEYWORDS =
      Set.of("severe bleeding", "profuse hemorrhage", "arterial bleeding", "uncontrolled bleeding");

  private static final Set<String> CRITICAL_NEUROLOGICAL_KEYWORDS =
      Set.of("seizure", "convulsion", "comatose", "unresponsive", "acute paralysis");

  private static final Set<String> CRITICAL_COLLAPSE_KEYWORDS =
      Set.of("collapse", "collapsed", "inability to stand", "downer cow", "anaphylaxis");

  private static final Set<String> NEGATION_PREFIXES =
      Set.of("no ", "denies ", "without ", "absent ", "not ", "free of ", "non-", "absence of ");

  /**
   * Evaluates deterministic safety rules against the triage request.
   *
   * @param request triage request parameters
   * @return {@link Optional} containing {@link TriageAssessment} if a deterministic emergency rule triggers, or empty
   */
  public Optional<TriageAssessment> evaluateRules(TriageRequest request) {
    if (request == null) {
      return Optional.empty();
    }

    List<String> detectedWarningSigns = new ArrayList<>();

    // 1. Evaluate symptoms
    if (request.symptoms() != null) {
      for (String symptom : request.symptoms()) {
        checkTextForEmergencyIndicators(symptom, detectedWarningSigns);
      }
    }

    // 2. Evaluate visual pathology observations
    if (request.diagnosisObservations() != null) {
      for (String obs : request.diagnosisObservations()) {
        checkTextForEmergencyIndicators(obs, detectedWarningSigns);
      }
    }

    // 3. Evaluate unified clinical evidence for CRITICAL status (Labs, Vitals, Sensors)
    if (request.unifiedEvidence() != null && request.unifiedEvidence().items() != null) {
      for (ClinicalEvidence item : request.unifiedEvidence().items()) {
        if (item.status() == AbnormalityStatus.CRITICAL) {
          String warningLabel = "Critical Multi-Modal Finding: " + item.summary();
          if (!detectedWarningSigns.contains(warningLabel)) {
            detectedWarningSigns.add(warningLabel);
          }
        }
      }
    }

    if (!detectedWarningSigns.isEmpty()) {
      log.warn(
          "DETERMINISTIC EMERGENCY RULE TRIGGERED: Warning signs detected={}",
          detectedWarningSigns);

      TriageAssessment emergencyAssessment =
          new TriageAssessment(
              TriageUrgency.EMERGENCY,
              BigDecimal.valueOf(1.00),
              "Deterministic safety rule triggered: Critical clinical indicator detected ("
                  + String.join("; ", detectedWarningSigns)
                  + "). Immediate emergency veterinary intervention is required.",
              detectedWarningSigns,
              List.of(
                  "Seek immediate emergency veterinary care without delay",
                  "Isolate animal and keep warm, quiet, and comfortable",
                  "Do not administer oral fluids or medications if animal is collapsed or unresponsive"),
              true,
              Instant.now());

      return Optional.of(emergencyAssessment);
    }

    return Optional.empty();
  }

  private void checkTextForEmergencyIndicators(String input, List<String> warningSigns) {
    if (input == null || input.isBlank()) {
      return;
    }

    String[] clauses = input.split("[,;.|\\n]");
    for (String clause : clauses) {
      String trimmed = clause.trim().toLowerCase();
      if (trimmed.isBlank()) {
        continue;
      }

      if (isNegated(trimmed)) {
        continue;
      }

      checkCategory(trimmed, CRITICAL_RESPIRATORY_KEYWORDS, "Critical Respiratory Distress", warningSigns);
      checkCategory(trimmed, CRITICAL_HEMORRHAGE_KEYWORDS, "Severe Uncontrolled Hemorrhage", warningSigns);
      checkCategory(trimmed, CRITICAL_NEUROLOGICAL_KEYWORDS, "Severe Neurological Dysfunction / Seizures", warningSigns);
      checkCategory(trimmed, CRITICAL_COLLAPSE_KEYWORDS, "Cardiovascular Collapse / Inability to Stand", warningSigns);
    }
  }

  private boolean isNegated(String phrase) {
    for (String prefix : NEGATION_PREFIXES) {
      if (phrase.startsWith(prefix) || phrase.contains(" " + prefix)) {
        return true;
      }
    }
    return false;
  }

  private void checkCategory(
      String phrase, Set<String> keywords, String warningLabel, List<String> warningSigns) {
    for (String kw : keywords) {
      if (phrase.contains(kw) && !warningSigns.contains(warningLabel)) {
        warningSigns.add(warningLabel);
        break;
      }
    }
  }
}
