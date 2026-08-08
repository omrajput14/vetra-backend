package app.vetra.ai.workflow.clinical.explainability;

import app.vetra.ai.rag.model.Citation;
import app.vetra.ai.rag.model.RetrievedContext;
import app.vetra.ai.workflow.clinical.model.ClinicalWorkflowContext;
import app.vetra.ai.workflow.clinical.model.DiseaseCandidate;
import app.vetra.ai.workflow.clinical.model.TreatmentPlan;
import app.vetra.ai.workflow.clinical.model.TriageAssessment;
import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import app.vetra.ai.workflow.clinical.model.evidence.AbnormalityStatus;
import app.vetra.ai.workflow.clinical.model.evidence.ClinicalEvidence;
import app.vetra.ai.workflow.clinical.model.evidence.EvidenceType;
import app.vetra.ai.workflow.clinical.model.evidence.UnifiedClinicalEvidence;
import app.vetra.ai.workflow.clinical.model.explainability.ClinicalDecisionSupport;
import app.vetra.ai.workflow.clinical.model.explainability.ClinicalUncertainty;
import app.vetra.ai.workflow.clinical.model.explainability.ContradictoryEvidenceSummary;
import app.vetra.ai.workflow.clinical.model.explainability.DiagnosticExplanation;
import app.vetra.ai.workflow.clinical.model.explainability.ReviewReasonCategory;
import app.vetra.ai.workflow.clinical.model.explainability.TriageExplanation;
import app.vetra.ai.workflow.clinical.model.explainability.TriageTriggerType;
import app.vetra.ai.workflow.clinical.model.explainability.TreatmentEvidence;
import app.vetra.ai.workflow.clinical.model.explainability.UncertaintyLevel;
import app.vetra.ai.workflow.clinical.model.explainability.VeterinarianReviewFlag;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Deterministic engine synthesizing explainability, evidence provenance, uncertainty,
 * and review flags from structured workflow outputs without invoking AI providers.
 */
@Component
public class ClinicalDecisionSupportEngine {

  private static final Logger log = LoggerFactory.getLogger(ClinicalDecisionSupportEngine.class);
  private static final String ENGINE_VERSION = "1.0.0";

  /**
   * Evaluates the shared {@link ClinicalWorkflowContext} and synthesizes a complete {@link ClinicalDecisionSupport}.
   *
   * @param context workflow execution context
   * @return deterministic {@link ClinicalDecisionSupport}
   */
  public ClinicalDecisionSupport evaluate(ClinicalWorkflowContext context) {
    if (context == null) {
      throw new IllegalArgumentException("ClinicalWorkflowContext cannot be null");
    }

    log.debug("ClinicalDecisionSupportEngine starting deterministic explainability evaluation");

    UnifiedClinicalEvidence unified = context.getUnifiedEvidence();
    List<DiseaseCandidate> candidates = context.getRankedDiseases();
    RetrievedContext retrievedContext = context.getRetrievedContext();
    TriageAssessment triage = context.getTriageAssessment();
    TreatmentPlan treatment = context.getTreatmentPlan();

    List<DiagnosticExplanation> diagnosticExplanations = buildDiagnosticExplanations(candidates, unified, retrievedContext);
    TriageExplanation triageExplanation = buildTriageExplanation(triage, unified);
    TreatmentEvidence treatmentEvidence = buildTreatmentEvidence(treatment, candidates, unified, retrievedContext);
    ClinicalUncertainty uncertainty = buildUncertainty(candidates, unified, retrievedContext);
    ContradictoryEvidenceSummary contradictions = buildContradictions(unified, candidates);
    VeterinarianReviewFlag reviewFlag = buildReviewFlag(triage, candidates, unified, uncertainty, contradictions, treatment);

    String primaryConclusion = buildPrimaryConclusion(candidates, triage, reviewFlag);
    Map<String, Object> safeAuditMetadata = buildSafeAuditMetadata(context);

    log.info(
        "ClinicalDecisionSupportEngine completed: uncertainty={}, requiresReview={}",
        uncertainty.overallLevel(),
        reviewFlag.requiresReview());

    return new ClinicalDecisionSupport(
        primaryConclusion,
        diagnosticExplanations,
        triageExplanation,
        treatmentEvidence,
        uncertainty,
        contradictions,
        reviewFlag,
        safeAuditMetadata,
        Instant.now());
  }

  private List<DiagnosticExplanation> buildDiagnosticExplanations(
      List<DiseaseCandidate> candidates,
      UnifiedClinicalEvidence unified,
      RetrievedContext retrievedContext) {

    if (candidates == null || candidates.isEmpty()) {
      return List.of();
    }

    Map<String, Double> normalizedContributions = calculateModalityContributions(unified, retrievedContext);
    List<DiagnosticExplanation> explanations = new ArrayList<>();

    for (DiseaseCandidate candidate : candidates) {
      String disease = candidate.diseaseName();
      List<ClinicalEvidence> supporting = new ArrayList<>();
      List<ClinicalEvidence> contradictory = new ArrayList<>();

      if (unified != null) {
        for (ClinicalEvidence evidence : unified.items()) {
          if (isEvidenceRelevantToCandidate(evidence, disease)) {
            if (evidence.status() == AbnormalityStatus.LOW) {
              contradictory.add(evidence);
            } else {
              supporting.add(evidence);
            }
          }
        }
      }

      List<Citation> citations = retrievedContext != null ? retrievedContext.citations() : List.of();
      UncertaintyLevel level = calculateCandidateUncertaintyLevel(candidate.confidence(), supporting.size());
      List<String> indicators = buildUncertaintyIndicators(candidate.confidence(), supporting.size(), level);

      explanations.add(
          new DiagnosticExplanation(
              disease,
              supporting,
              contradictory,
              citations,
              normalizedContributions,
              candidate.confidence(),
              level,
              indicators));
    }
    return explanations;
  }

  private Map<String, Double> calculateModalityContributions(UnifiedClinicalEvidence unified, RetrievedContext retrievedContext) {
    double visionWeight = 0.35;
    double labWeight = 0.25;
    double vitalWeight = 0.15;
    double symptomWeight = 0.15;
    double ragWeight = 0.10;

    Map<String, Double> active = new LinkedHashMap<>();
    active.put("IMAGE", visionWeight);

    if (unified != null) {
      if (!unified.findByType(EvidenceType.SYMPTOM).isEmpty()) {
        active.put("SYMPTOM", symptomWeight);
      }
      if (!unified.findByType(EvidenceType.LAB_RESULT).isEmpty()) {
        active.put("LAB_RESULT", labWeight);
      }
      if (!unified.findByType(EvidenceType.VITAL_SIGN).isEmpty()) {
        active.put("VITAL_SIGN", vitalWeight);
      }
    }
    if (retrievedContext != null && retrievedContext.totalChunks() > 0) {
      active.put("RAG_LITERATURE", ragWeight);
    }

    double totalActive = active.values().stream().mapToDouble(Double::doubleValue).sum();
    Map<String, Double> normalized = new LinkedHashMap<>();
    for (Map.Entry<String, Double> entry : active.entrySet()) {
      double norm = totalActive > 0 ? (entry.getValue() / totalActive) : 0.0;
      normalized.put(entry.getKey(), BigDecimal.valueOf(norm).setScale(4, RoundingMode.HALF_UP).doubleValue());
    }
    return normalized;
  }

  private boolean isEvidenceRelevantToCandidate(ClinicalEvidence evidence, String candidateName) {
    if (evidence == null || candidateName == null || candidateName.isBlank()) {
      return false;
    }
    String target = candidateName.toLowerCase();
    String summary = evidence.summary().toLowerCase();
    return summary.contains(target) || evidence.observations().stream().anyMatch(f -> f.toLowerCase().contains(target));
  }

  private UncertaintyLevel calculateCandidateUncertaintyLevel(BigDecimal confidence, int supportingEvidenceCount) {
    double conf = confidence != null ? confidence.doubleValue() : 0.0;
    if (conf >= 0.85 && supportingEvidenceCount >= 1) {
      return UncertaintyLevel.HIGH_CONFIDENCE;
    } else if (conf >= 0.60) {
      return UncertaintyLevel.MODERATE_CONFIDENCE;
    } else if (conf >= 0.40) {
      return UncertaintyLevel.LOW_CONFIDENCE;
    } else {
      return UncertaintyLevel.INSUFFICIENT_EVIDENCE;
    }
  }

  private List<String> buildUncertaintyIndicators(BigDecimal confidence, int supportingCount, UncertaintyLevel level) {
    List<String> indicators = new ArrayList<>();
    if (confidence.doubleValue() < 0.60) {
      indicators.add(String.format("Diagnostic confidence is below 60%% (Score: %.2f)", confidence.doubleValue()));
    }
    if (supportingCount == 0) {
      indicators.add("No direct specific evidence items matched candidate condition");
    }
    if (level == UncertaintyLevel.INSUFFICIENT_EVIDENCE) {
      indicators.add("Insufficient multi-modal evidence available for conclusive diagnosis");
    }
    return indicators;
  }

  private TriageExplanation buildTriageExplanation(TriageAssessment triage, UnifiedClinicalEvidence unified) {
    if (triage == null) {
      return new TriageExplanation(TriageUrgency.ROUTINE, TriageTriggerType.AI_ASSESSMENT, List.of(), List.of(), "Default routine monitoring");
    }

    boolean isDeterministicEmergency =
        triage.urgency() == TriageUrgency.EMERGENCY
            && (triage.rationale().toLowerCase().contains("deterministic")
                || triage.rationale().toLowerCase().contains("emergency rule")
                || triage.warningSigns().stream().anyMatch(w -> w.toLowerCase().contains("emergency")));

    TriageTriggerType triggerType =
        isDeterministicEmergency ? TriageTriggerType.DETERMINISTIC_SAFETY_RULE : TriageTriggerType.AI_ASSESSMENT;

    List<String> rules = isDeterministicEmergency ? triage.warningSigns() : List.of();
    List<String> factors = triage.warningSigns();
    String rationale =
        isDeterministicEmergency
            ? "Deterministic clinical safety rule triggered emergency escalation"
            : triage.rationale();

    return new TriageExplanation(triage.urgency(), triggerType, rules, factors, rationale);
  }

  private TreatmentEvidence buildTreatmentEvidence(
      TreatmentPlan treatment,
      List<DiseaseCandidate> candidates,
      UnifiedClinicalEvidence unified,
      RetrievedContext retrievedContext) {

    List<String> targetedDiseases = candidates != null ? candidates.stream().map(DiseaseCandidate::diseaseName).toList() : List.of();
    List<ClinicalEvidence> supportingEvidence = unified != null ? unified.items() : List.of();
    List<Citation> citations = retrievedContext != null ? retrievedContext.citations() : List.of();
    List<String> warnings = treatment != null ? treatment.precautions() : List.of();

    return new TreatmentEvidence(targetedDiseases, supportingEvidence, citations, warnings);
  }

  private ClinicalUncertainty buildUncertainty(
      List<DiseaseCandidate> candidates,
      UnifiedClinicalEvidence unified,
      RetrievedContext retrievedContext) {

    BigDecimal topConfidence = (candidates != null && !candidates.isEmpty())
        ? candidates.get(0).confidence()
        : BigDecimal.ZERO;

    List<String> missingModalities = identifyMissingModalities(unified, retrievedContext);
    UncertaintyLevel level = determineUncertaintyTier(topConfidence.doubleValue(), missingModalities.size());

    List<String> factors = new ArrayList<>();
    if (!missingModalities.isEmpty()) {
      factors.add("Missing clinical modalities: " + String.join(", ", missingModalities));
    }
    if (topConfidence.doubleValue() < 0.60) {
      factors.add("Top candidate confidence is below 60%");
    }

    return new ClinicalUncertainty(level, topConfidence, factors, missingModalities);
  }

  private List<String> identifyMissingModalities(UnifiedClinicalEvidence unified, RetrievedContext retrievedContext) {
    List<String> missing = new ArrayList<>();
    if (unified == null || unified.findByType(EvidenceType.LAB_RESULT).isEmpty()) {
      missing.add("LAB_RESULT");
    }
    if (unified == null || unified.findByType(EvidenceType.VITAL_SIGN).isEmpty()) {
      missing.add("VITAL_SIGN");
    }
    if (unified == null || unified.findByType(EvidenceType.SENSOR_OBSERVATION).isEmpty()) {
      missing.add("SENSOR_OBSERVATION");
    }
    if (unified == null || unified.findByType(EvidenceType.CLINICAL_HISTORY).isEmpty()) {
      missing.add("CLINICAL_HISTORY");
    }
    if (retrievedContext == null || retrievedContext.totalChunks() == 0) {
      missing.add("RAG_LITERATURE");
    }
    return missing;
  }

  private UncertaintyLevel determineUncertaintyTier(double topConfidence, int missingCount) {
    if (topConfidence < 0.40 || missingCount >= 4) {
      return UncertaintyLevel.INSUFFICIENT_EVIDENCE;
    } else if (topConfidence < 0.60) {
      return UncertaintyLevel.LOW_CONFIDENCE;
    } else if (topConfidence < 0.85) {
      return UncertaintyLevel.MODERATE_CONFIDENCE;
    } else {
      return UncertaintyLevel.HIGH_CONFIDENCE;
    }
  }

  private ContradictoryEvidenceSummary buildContradictions(
      UnifiedClinicalEvidence unified,
      List<DiseaseCandidate> candidates) {

    List<String> conflicts = unified != null ? unified.conflicts() : List.of();
    List<ClinicalEvidence> contradictions = new ArrayList<>();
    List<String> literatureDiscrepancies = new ArrayList<>();

    if (unified != null && candidates != null && !candidates.isEmpty()) {
      String topDisease = candidates.get(0).diseaseName().toLowerCase();
      for (ClinicalEvidence item : unified.items()) {
        if (item.status() == AbnormalityStatus.LOW && item.summary().toLowerCase().contains(topDisease)) {
          contradictions.add(item);
        }
      }
    }

    return new ContradictoryEvidenceSummary(conflicts, contradictions, literatureDiscrepancies);
  }

  private VeterinarianReviewFlag buildReviewFlag(
      TriageAssessment triage,
      List<DiseaseCandidate> candidates,
      UnifiedClinicalEvidence unified,
      ClinicalUncertainty uncertainty,
      ContradictoryEvidenceSummary contradictions,
      TreatmentPlan treatment) {

    List<String> reasons = new ArrayList<>();
    List<ReviewReasonCategory> categories = new ArrayList<>();

    checkUrgencyAndConfidence(triage, candidates, reasons, categories);
    checkEvidenceQuality(unified, uncertainty, contradictions, reasons, categories);
    checkTreatmentPrecautions(treatment, reasons, categories);

    boolean requiresReview = !categories.isEmpty();
    return new VeterinarianReviewFlag(requiresReview, reasons, categories);
  }

  private void checkUrgencyAndConfidence(
      TriageAssessment triage,
      List<DiseaseCandidate> candidates,
      List<String> reasons,
      List<ReviewReasonCategory> categories) {

    if (triage != null && (triage.urgency() == TriageUrgency.EMERGENCY || triage.urgency() == TriageUrgency.URGENT)) {
      reasons.add("Case evaluated with " + triage.urgency() + " urgency");
      categories.add(ReviewReasonCategory.EMERGENCY_TRIAGE);
    }
    if (candidates == null || candidates.isEmpty() || candidates.get(0).confidence().doubleValue() < 0.60) {
      reasons.add("Diagnostic confidence is below 60%");
      categories.add(ReviewReasonCategory.LOW_DIAGNOSTIC_CONFIDENCE);
    }
  }

  private void checkEvidenceQuality(
      UnifiedClinicalEvidence unified,
      ClinicalUncertainty uncertainty,
      ContradictoryEvidenceSummary contradictions,
      List<String> reasons,
      List<ReviewReasonCategory> categories) {

    if (uncertainty.overallLevel() == UncertaintyLevel.INSUFFICIENT_EVIDENCE) {
      reasons.add("Insufficient clinical evidence streams available");
      categories.add(ReviewReasonCategory.INSUFFICIENT_EVIDENCE);
    }
    if (contradictions != null && !contradictions.conflictingMeasurements().isEmpty()) {
      reasons.add("Genuine measurement conflicts detected across evidence streams");
      categories.add(ReviewReasonCategory.EVIDENCE_CONFLICT);
    }
    if (unified != null && hasCriticalAbnormality(unified)) {
      reasons.add("Critical laboratory or vital sign abnormalities detected");
      categories.add(ReviewReasonCategory.CRITICAL_LAB_OR_VITAL);
    }
  }

  private boolean hasCriticalAbnormality(UnifiedClinicalEvidence unified) {
    return unified.items().stream()
        .anyMatch(e -> e.status() == AbnormalityStatus.CRITICAL || e.status() == AbnormalityStatus.HIGH);
  }

  private void checkTreatmentPrecautions(
      TreatmentPlan treatment,
      List<String> reasons,
      List<ReviewReasonCategory> categories) {

    if (treatment != null && treatment.precautions() != null && !treatment.precautions().isEmpty()) {
      reasons.add("Treatment precautions or medication monitoring required");
      categories.add(ReviewReasonCategory.TREATMENT_WARNING);
    }
  }

  private String buildPrimaryConclusion(
      List<DiseaseCandidate> candidates,
      TriageAssessment triage,
      VeterinarianReviewFlag reviewFlag) {

    String top = (candidates != null && !candidates.isEmpty())
        ? candidates.get(0).diseaseName() + " (" + candidates.get(0).confidence() + ")"
        : "Unspecified Clinical Observation";
    String urg = triage != null ? triage.urgency().name() : "ROUTINE";
    String review = reviewFlag.requiresReview() ? "VETERINARIAN REVIEW REQUIRED" : "Routine Monitoring";

    return String.format("Primary Candidate: %s | Triage: %s | Status: %s", top, urg, review);
  }

  private Map<String, Object> buildSafeAuditMetadata(ClinicalWorkflowContext context) {
    Map<String, Object> safeMeta = new HashMap<>();
    safeMeta.put("engineVersion", ENGINE_VERSION);
    safeMeta.put("workflowStepOrder", 7);
    safeMeta.put("explanationStrategy", "DETERMINISTIC_EVIDENCE_TRACING");
    safeMeta.put("evaluatedAt", Instant.now().toString());
    safeMeta.put("totalStepCount", 8);
    return safeMeta;
  }
}
